package moe.mewore.rabbit.backend.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.Synchronized;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;
import moe.mewore.rabbit.backend.simulation.player.RabbitPlayer;

public class RealtimeSimulation {

    private static final int MAX_INPUT_FRAME_SHIFT = 15;

    static final int FPS = 60;

    private static final int FUTURE_FRAME_BUFFER = 20;

    private static final int MAXIMUM_ROLLBACK_MILLISECONDS = 1000;

    private static final int FRAME_BUFFER_SIZE =
        Math.max(MAXIMUM_ROLLBACK_MILLISECONDS * 2, 5000) * FPS / 1000 + FUTURE_FRAME_BUFFER;

    private final byte[][] frames;

    private final long createdAt = System.currentTimeMillis();

    private final RabbitWorldState state;

    private int frameIndex = 0;

    private @Nullable Integer rollbackOffset = null;

    private final BlockingQueue<PlayerInputEvent> pendingInputs = new LinkedBlockingDeque<>();

    private final @Nullable PlayerInputEvent[][] playerInputHistory;

    @Getter
    private @Nullable List<PlayerInputEvent> lastAppliedInputs;

    public RealtimeSimulation(final RabbitWorldState state) {
        this.state = state;
        frames = new byte[FRAME_BUFFER_SIZE][state.getFrameSize()];
        playerInputHistory = new @Nullable PlayerInputEvent[FRAME_BUFFER_SIZE][state.getMaxPlayerCount()];
        System.out.println("Memory used for the frames: " +
            FRAME_BUFFER_SIZE * (state.getFrameSize() + state.getMaxPlayerCount() * 8) / 1024 + " KB");
    }

    public void acceptInput(final RabbitPlayer player, final PlayerInput input) throws InterruptedException {
        final PlayerInputEvent lastInputEvent = player.getLastInputEvent();
        if (lastInputEvent != null && lastInputEvent.getInput().getId() >= input.getId()) {
            return;
        }
        pendingInputs.put(new PlayerInputEvent(player.getId(), player.getUid(), input));
    }

    @Synchronized
    private void applyInput(final PlayerInputEvent inputEvent) {
        final PlayerInput input = inputEvent.getInput();

        long inputFrame = inputEvent.getFrameId();
        inputFrame = Math.abs(input.getFrameId() - inputFrame) <= MAX_INPUT_FRAME_SHIFT
            ? input.getFrameId()
            : inputFrame + (inputFrame > input.getFrameId() ? -MAX_INPUT_FRAME_SHIFT : MAX_INPUT_FRAME_SHIFT);

        final long currentFrame = state.getFrameId();
        inputFrame = Math.min(currentFrame + FUTURE_FRAME_BUFFER,
            Math.max(currentFrame - FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2, Math.max(1, inputFrame)));

        if (inputFrame != input.getFrameId()) {
            System.out.printf(
                "[#%d] Input #%d cannot be applied to its desired frame #%d; instead, it will be applied to #%d%n",
                currentFrame, input.getId(), input.getFrameId(), inputFrame);
            inputEvent.setInput(input.withFrameId(inputFrame));
        }

        final int frameOffset = (int) (inputFrame - currentFrame);
        assert frameOffset >= -FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2 && frameOffset <= FUTURE_FRAME_BUFFER :
            " frameOffset(" + frameOffset + ") is supposed to be in the range [" +
                (-FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2) + ", " + FUTURE_FRAME_BUFFER + "]";

        final int inputFrameIndex = (frameIndex + frameOffset + FRAME_BUFFER_SIZE) % FRAME_BUFFER_SIZE;

        if (frameOffset < 0 && (rollbackOffset == null || frameOffset < rollbackOffset)) {
            rollbackOffset = frameOffset;
        }

        System.out.printf("Applying input %d to frame %d (now = %d)%n", input.getId(), inputFrame, currentFrame);
        final int playerId = inputEvent.getPlayerId();
        if (inputEvent.canReplace(playerInputHistory[inputFrameIndex][playerId])) {
            playerInputHistory[inputFrameIndex][playerId] = inputEvent;
        }
    }

    public byte[] getCurrentSnapshot() {
        return frames[frameIndex];
    }

    @Synchronized
    public byte[] getPastSnapshot(final int millisecondsInPast) {
        final int maxFrameDifference = (int) Math.min(state.getFrameId(), FRAME_BUFFER_SIZE);
        final int frameDifference = Math.min(maxFrameDifference, millisecondsInPast * FPS / 1000);
        return frames[(frameDifference <= frameIndex ? frameIndex : frameIndex + FRAME_BUFFER_SIZE) - frameDifference];
    }

    @Synchronized
    public RabbitWorldState update(final long now) {
        if (!pendingInputs.isEmpty()) {
            final List<PlayerInputEvent> inputsToApply = new ArrayList<>(pendingInputs.size());
            pendingInputs.drainTo(inputsToApply);
            for (final PlayerInputEvent input : inputsToApply) {
                applyInput(input);
            }
            lastAppliedInputs = inputsToApply.stream()
                .sorted(Comparator.comparingLong(PlayerInputEvent::getFrameId))
                .collect(Collectors.toUnmodifiableList());
        } else {
            lastAppliedInputs = null;
        }

        final long targetFrame = Math.round((now - createdAt) * FPS * .001);
        if (rollbackOffset != null) {
            final int replayFrameIndex = (frameIndex + rollbackOffset + FRAME_BUFFER_SIZE) % FRAME_BUFFER_SIZE;
            final int frameToStopAt = (frameIndex + 1) % FRAME_BUFFER_SIZE;
            state.load(frames[replayFrameIndex]);
            state.loadInput(playerInputHistory[replayFrameIndex], true);

            for (int i = (replayFrameIndex + 1) % FRAME_BUFFER_SIZE;
                 i != frameToStopAt; i = (i + 1) % FRAME_BUFFER_SIZE) {
                state.loadInput(playerInputHistory[i], false);
                state.doStep();
                state.store(frames[i]);
                state.storeInput(playerInputHistory[i]);
            }
            rollbackOffset = null;
        }
        long steps = targetFrame - state.getFrameId();
        while (--steps >= 0) {
            state.loadInput(playerInputHistory[frameIndex], false);
            state.doStep();
            frameIndex = (frameIndex + 1) % FRAME_BUFFER_SIZE;
            state.store(frames[frameIndex]);
            state.storeInput(playerInputHistory[frameIndex]);
        }

        return state;
    }

}
