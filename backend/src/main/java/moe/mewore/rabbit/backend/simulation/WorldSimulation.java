package moe.mewore.rabbit.backend.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;

public class WorldSimulation {

    private static final int MAX_INPUT_FRAME_SHIFT = 15;

    static final int FPS = 60;

    private static final int FUTURE_FRAME_BUFFER = 20;

    private static final int MAXIMUM_ROLLBACK_MILLISECONDS = 1000;

    private static final int FRAME_BUFFER_SIZE =
        Math.max(MAXIMUM_ROLLBACK_MILLISECONDS * 2, 5000) * FPS / 1000 + FUTURE_FRAME_BUFFER;

    private final WorldSnapshot[] frames = new WorldSnapshot[FRAME_BUFFER_SIZE];

    private final long createdAt = System.currentTimeMillis();

    private final WorldState state;

    private int frameIndex = 0;

    private @Nullable Integer rollbackOffset = null;

    private final BlockingQueue<PendingInput> pendingInputs = new LinkedBlockingDeque<>();

    public WorldSimulation(final WorldState state) {
        this.state = state;
        for (int i = 0; i < FRAME_BUFFER_SIZE; i++) {
            frames[i] = state.createEmptySnapshot();
        }
        System.out.println(
            "Memory used for the frames: " + FRAME_BUFFER_SIZE * WorldState.BYTES_PER_STORED_STATE / 1024 + " KB");
    }

    public void acceptInput(final Player player, final PlayerInputMutation input) throws InterruptedException {
        final long inputTimestamp =
            System.currentTimeMillis() - createdAt - Math.min(player.getLatency(), MAXIMUM_ROLLBACK_MILLISECONDS);
        final long expectedInputFrame = Math.round(inputTimestamp * FPS * .001);
        pendingInputs.put(new PendingInput(player, input, inputTimestamp, expectedInputFrame));
    }

    @Synchronized
    private void applyInput(final PendingInput pendingInput) {
        final Player player = pendingInput.getPlayer();
        final PlayerInputMutation input = pendingInput.getInput();

        long inputFrame = pendingInput.getTargetFrame();
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
            System.out.printf("\t- Input timestamp: %d   |   Expected input frame: %d   |   Latency: %d%n",
                pendingInput.getSupposedTimestamp(), pendingInput.getTargetFrame(), player.getLatency());
        }

        final int frameOffset = (int) (inputFrame - currentFrame);
        assert frameOffset >= -FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2 && frameOffset <= FUTURE_FRAME_BUFFER :
            " frameOffset(" + frameOffset + ") is supposed to be in the range [" +
                (-FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2) + ", " + FUTURE_FRAME_BUFFER + "]";

        final int inputFrameIndex = (frameIndex + frameOffset + FRAME_BUFFER_SIZE) % FRAME_BUFFER_SIZE;

        if (frameOffset < 0 && (rollbackOffset == null || frameOffset < rollbackOffset)) {
            rollbackOffset = frameOffset;
        }
        WorldState.registerInput(frames[inputFrameIndex], player, input);
    }

    public WorldSnapshot getCurrentSnapshot() {
        return frames[frameIndex];
    }

    @Synchronized
    public WorldSnapshot getPastSnapshot(final int millisecondsInPast) {
        final int maxFrameDifference = (int) Math.min(state.getFrameId(), FRAME_BUFFER_SIZE);
        final int frameDifference = Math.min(maxFrameDifference, millisecondsInPast * FPS / 1000);
        return frames[(frameDifference <= frameIndex ? frameIndex : frameIndex + FRAME_BUFFER_SIZE) - frameDifference];
    }

    @Synchronized
    public WorldState update(final long now) {
        if (!pendingInputs.isEmpty()) {
            final List<PendingInput> inputsToApply = new ArrayList<>(pendingInputs.size());
            pendingInputs.drainTo(inputsToApply);
            for (final PendingInput input : inputsToApply) {
                applyInput(input);
            }
        }

        if (rollbackOffset != null) {
            final int replayFrameIndex = (frameIndex + rollbackOffset + FRAME_BUFFER_SIZE) % FRAME_BUFFER_SIZE;
            final int frameToStopAt = (frameIndex + 1) % FRAME_BUFFER_SIZE;
            state.load(frames[replayFrameIndex]);
            for (int i = (replayFrameIndex + 1) % FRAME_BUFFER_SIZE;
                 i != frameToStopAt; i = (i + 1) % FRAME_BUFFER_SIZE) {
                state.doStep();
                state.loadInput(frames[i]);
                state.store(frames[i]);
            }
            rollbackOffset = null;
        }
        final long targetFrame = Math.round((now - createdAt) * FPS * .001);
        long steps = targetFrame - state.getFrameId();
        while (--steps >= 0) {
            state.loadInput(frames[frameIndex]);
            state.doStep();
            state.store(frames[frameIndex = (frameIndex + 1) % FRAME_BUFFER_SIZE]);
        }

        return state;
    }

    @Getter
    @RequiredArgsConstructor
    private static class PendingInput {

        private final Player player;

        private final PlayerInputMutation input;

        private final long supposedTimestamp;

        private final long targetFrame;
    }
}
