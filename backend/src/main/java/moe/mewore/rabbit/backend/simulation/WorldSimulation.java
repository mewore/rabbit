package moe.mewore.rabbit.backend.simulation;

import lombok.Synchronized;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;

public class WorldSimulation {

    private static final int MAX_INPUT_FRAME_SHIFT = 15;

    static final int FPS = 60;

    private static final int FUTURE_FRAME_BUFFER = 20;

    private static final int MAXIMUM_ROLLBACK_MILLISECONDS = 1000;

    private static final int FRAME_BUFFER_SIZE = MAXIMUM_ROLLBACK_MILLISECONDS * 2 * FPS / 1000 + FUTURE_FRAME_BUFFER;

    private final WorldSnapshot[] frames = new WorldSnapshot[FRAME_BUFFER_SIZE];

    private final long createdAt = System.currentTimeMillis();

    private final WorldState state;

    private int frameIndex = 0;

    private int frameToReplayFrom = -1;

    public WorldSimulation(final WorldState state) {
        this.state = state;
        for (int i = 0; i < FRAME_BUFFER_SIZE; i++) {
            frames[i] = state.createEmptySnapshot();
        }
        System.out.println(
            "Memory used for the frames: " + FRAME_BUFFER_SIZE * WorldState.BYTES_PER_STORED_STATE / 1024 + " KB");
    }

    // TODO: Make it less synchronized
    @Synchronized
    public synchronized void acceptInput(final Player player, final PlayerInputMutation input) {
        final int inputTimestamp = (int) (System.currentTimeMillis() - createdAt) -
            Math.min(player.getLatency(), MAXIMUM_ROLLBACK_MILLISECONDS);
        final int expectedInputFrame = Math.round(inputTimestamp * FPS * .001f);
        int inputFrame = expectedInputFrame;
        inputFrame = Math.abs(input.getFrameId() - inputFrame) <= MAX_INPUT_FRAME_SHIFT
            ? input.getFrameId()
            : inputFrame + (inputFrame > input.getFrameId() ? -MAX_INPUT_FRAME_SHIFT : MAX_INPUT_FRAME_SHIFT);

        final int currentFrame = state.getFrameId();
        inputFrame = Math.min(currentFrame + FUTURE_FRAME_BUFFER,
            Math.max(currentFrame - FRAME_BUFFER_SIZE + FUTURE_FRAME_BUFFER + 2, Math.max(1, inputFrame)));
        if (inputFrame != input.getFrameId()) {
            System.out.printf(
                "[#%d] Input #%d cannot be applied to its desired frame #%d; instead, it will be applied to #%d%n",
                currentFrame, input.getId(), input.getFrameId(), inputFrame);
            System.out.printf("\t- Input timestamp: %d   |   Expected input frame: %d   |   Latency: %d%n",
                inputTimestamp, expectedInputFrame, player.getLatency());
        }

        final int frameDifference = Math.abs(currentFrame - inputFrame);
        final int inputFrameIndex = inputFrame <= currentFrame ? frameDifference <= frameIndex
            ? (frameIndex - frameDifference)
            : (frameIndex + FRAME_BUFFER_SIZE - frameDifference) : (frameIndex + frameDifference) % FRAME_BUFFER_SIZE;

        if (inputFrame < currentFrame && (frameToReplayFrom == -1 || inputFrame < frameToReplayFrom)) {
            frameToReplayFrom = inputFrame;
        }
        WorldState.registerInput(frames[inputFrameIndex], player, input);
    }

    // TODO: Make it less synchronized
    @Synchronized
    public WorldState update(final long now) {
        if (frameToReplayFrom > -1) {
            final int replayFrameIndex =
                (frameIndex - (state.getFrameId() - frameToReplayFrom) + FRAME_BUFFER_SIZE) % FRAME_BUFFER_SIZE;
            final int frameToStopAt = (frameIndex + 1) % FRAME_BUFFER_SIZE;
            state.load(frames[replayFrameIndex], frameToReplayFrom);
            for (int i = (replayFrameIndex + 1) % FRAME_BUFFER_SIZE;
                 i != frameToStopAt; i = (i + 1) % FRAME_BUFFER_SIZE) {
                state.doStep();
                state.loadInput(frames[i]);
                state.store(frames[i]);
            }
            frameToReplayFrom = -1;
        }
        final int targetFrame = Math.round((now - createdAt) * FPS * .001f);
        while (state.getFrameId() < targetFrame) {
            int steps = targetFrame - state.getFrameId();
            while (--steps >= 0) {
                state.loadInput(frames[frameIndex]);
                state.doStep();
                state.store(frames[frameIndex = (frameIndex + 1) % FRAME_BUFFER_SIZE]);
            }
        }

        return state;
    }
}
