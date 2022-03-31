package moe.mewore.rabbit.backend.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.Synchronized;
import moe.mewore.rabbit.backend.simulation.player.Player;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;

public class RealtimeSimulation<I extends PlayerInput> {

    /**
     * A comparator which compares inputs based on their frame ID. If used in a priority queue (heap), the input with
     * the lowest frame ID, i.e., the oldest one, should be at the top.
     */
    private static final Comparator<PlayerInput> INPUT_FRAME_COMPARATOR = Comparator.comparingLong(
        PlayerInput::getFrameId);

    private static final Comparator<PlayerInputEvent<?>> INPUT_EVENT_FRAME_COMPARATOR = Comparator.comparingLong(
        PlayerInputEvent::getFrameId);

    private static final int MAX_INPUT_FRAME_SHIFT = 15;

    private static final int FPS = 60;

    private static final float SECONDS_PER_FRAME = 1f / FPS;

    private static final int FUTURE_FRAME_BUFFER = 20;

    private static final int MAXIMUM_ROLLBACK_MILLISECONDS = 1000;

    private static final int FRAME_BUFFER_SIZE =
        (MAXIMUM_ROLLBACK_MILLISECONDS * 2 + 500) * FPS / 1000 + FUTURE_FRAME_BUFFER;

    private final byte[][] frames;

    private final long createdAt = System.currentTimeMillis();

    private final World<I, ?> world;

    private int frameIndex = 0;

    private @Nullable Integer rollbackOffset = null;

    private final BlockingQueue<PlayerInputEvent<I>> pendingInputs = new LinkedBlockingDeque<>();

    /**
     * Inputs that may have an effect during a rollback.
     */
    private final List<Queue<I>> relevantInputsByPlayerId;

    /**
     * Inputs that have yet to be applied to the player entities.
     */
    private final List<Queue<I>> pendingInputsByPlayerId;

    @Getter
    private @Nullable List<PlayerInputEvent<I>> lastAppliedInputs;

    public RealtimeSimulation(final World<I, ?> world) {
        this.world = world;
        frames = new byte[FRAME_BUFFER_SIZE][world.getFrameSize()];
        System.out.println("Memory used for the frames: " +
            FRAME_BUFFER_SIZE * (world.getFrameSize() + world.getMaxPlayerCount() * 8) / 1024 + " KB");

        relevantInputsByPlayerId = new ArrayList<>(world.getMaxPlayerCount());
        pendingInputsByPlayerId = new ArrayList<>(world.getMaxPlayerCount());
        for (int i = 0; i < world.getMaxPlayerCount(); i++) {
            relevantInputsByPlayerId.add(new PriorityQueue<>(INPUT_FRAME_COMPARATOR));
            pendingInputsByPlayerId.add(new PriorityQueue<>(INPUT_FRAME_COMPARATOR));
        }
    }

    public void acceptInput(final Player<? extends I> player, final I input) throws InterruptedException {
        pendingInputs.put(new PlayerInputEvent<>(player.getIndex(), player.getUid(), input));
    }

    @Synchronized
    private void applyInputEvent(final PlayerInputEvent<I> inputEvent) {
        final I input = inputEvent.getInput();
        restrictInputFrame(input);

        final int frameOffset = (int) (input.getFrameId() - world.getFrameId());
        assert frameOffset >= -FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2 && frameOffset <= FUTURE_FRAME_BUFFER :
            " frameOffset(" + frameOffset + ") is supposed to be in the range [" +
                (-FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2) + ", " + FUTURE_FRAME_BUFFER + "]";

        if (frameOffset < 0 && (rollbackOffset == null || frameOffset < rollbackOffset)) {
            rollbackOffset = frameOffset;
        }

        final int playerId = inputEvent.getPlayerId();
        final Queue<I> relevantInputs = relevantInputsByPlayerId.get(playerId);
        relevantInputs.add(input);

        // Clean the old inputs that shouldn't matter anymore, but always keep at least one input available
        final long minimumInputFrame = world.getFrameId() - FRAME_BUFFER_SIZE;
        @Nullable I oldestInput = relevantInputs.peek();
        while (oldestInput != null && relevantInputs.size() > 1 && oldestInput.getFrameId() < minimumInputFrame) {
            relevantInputs.remove();
            oldestInput = relevantInputs.peek();
        }

        pendingInputsByPlayerId.get(playerId).add(input);
    }

    private void restrictInputFrame(final I input) {
        long inputFrame = input.getFrameId();
        inputFrame = Math.abs(input.getFrameId() - inputFrame) <= MAX_INPUT_FRAME_SHIFT
            ? input.getFrameId()
            : inputFrame + (inputFrame > input.getFrameId() ? -MAX_INPUT_FRAME_SHIFT : MAX_INPUT_FRAME_SHIFT);

        final long currentFrame = world.getFrameId();
        inputFrame = Math.min(currentFrame + FUTURE_FRAME_BUFFER,
            Math.max(currentFrame - FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2, Math.max(0, inputFrame)));

        if (inputFrame != input.getFrameId()) {
            System.out.printf(
                "[#%d] Input #%d cannot be applied to its desired frame #%d; instead, it will be applied to #%d%n",
                currentFrame, input.getId(), input.getFrameId(), inputFrame);
            input.setFrameId(inputFrame);
        }
    }

    public byte[] getCurrentSnapshot() {
        return frames[frameIndex];
    }

    @Synchronized
    public byte[] getPastSnapshot(final int millisecondsInPast) {
        final int maxFrameDifference = (int) Math.min(world.getFrameId(), FRAME_BUFFER_SIZE);
        final int frameDifference = Math.min(maxFrameDifference, millisecondsInPast * FPS / 1000);
        return frames[(frameDifference <= frameIndex ? frameIndex : frameIndex + FRAME_BUFFER_SIZE) - frameDifference];
    }

    public void advanceToFrame(final long targetFrame) {
        if (!pendingInputs.isEmpty()) {
            final List<PlayerInputEvent<I>> inputEventsToApply = new ArrayList<>(pendingInputs.size());
            pendingInputs.drainTo(inputEventsToApply);
            for (final PlayerInputEvent<I> inputEvent : inputEventsToApply) {
                applyInputEvent(inputEvent);
            }
            lastAppliedInputs = inputEventsToApply.stream()
                .sorted(INPUT_EVENT_FRAME_COMPARATOR)
                .collect(Collectors.toUnmodifiableList());
        } else {
            lastAppliedInputs = null;
        }

        if (rollbackOffset != null) {
            world.forEachPlayer(player -> pendingInputsByPlayerId.set(player.getIndex(),
                new PriorityQueue<>(relevantInputsByPlayerId.get(player.getIndex()))));
            frameIndex = (frameIndex + rollbackOffset + FRAME_BUFFER_SIZE) % FRAME_BUFFER_SIZE;
            world.load(frames[frameIndex]);
            world.applyInputs(pendingInputsByPlayerId, true);
            rollbackOffset = null;
        }

        while (world.getFrameId() < targetFrame) {
            world.applyInputs(pendingInputsByPlayerId, false);
            world.doStep(SECONDS_PER_FRAME);
            frameIndex = (frameIndex + 1) % FRAME_BUFFER_SIZE;
            world.store(frames[frameIndex]);
        }
    }

    @Synchronized
    public void update(final long now) {
        advanceToFrame(Math.round((now - createdAt) * FPS * .001));
    }
}
