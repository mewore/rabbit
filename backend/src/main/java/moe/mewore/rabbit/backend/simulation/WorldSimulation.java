package moe.mewore.rabbit.backend.simulation;

import lombok.Synchronized;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;

public class WorldSimulation {

    private static final int FPS = 60;

    private static final int MILLISECONDS_PER_FRAME = 1000 / FPS;

    private static final double SECONDS_PER_FRAME = MILLISECONDS_PER_FRAME / 1000.0;

    private static final int MAXIMUM_ROLLBACK_MILLISECONDS = 1000;

    private static final int FRAME_BUFFER_SIZE = MAXIMUM_ROLLBACK_MILLISECONDS * 2 * FPS / 1000;

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

    private int getLowerIndex(final int first, final int second) {
        return (first <= frameIndex ? first + FRAME_BUFFER_SIZE : first) <=
            (second <= frameIndex ? second + FRAME_BUFFER_SIZE : second) ? first : second;
    }

    // TODO: Make it less synchronized
    @Synchronized
    public synchronized void acceptInput(final Player player, final PlayerInputMutation input) {
        final int inputTimestamp = (int) (System.currentTimeMillis() - createdAt) -
            Math.min(player.getLatency(), MAXIMUM_ROLLBACK_MILLISECONDS);
        int from = frameIndex + 1;
        int newReplayIndex = from % FRAME_BUFFER_SIZE;
        if (frames[newReplayIndex].getTimestamp() > inputTimestamp) {
            // The input is older than the oldest frame timestamp
            // This should never happen
            System.err.printf(
                "An input sent by player %d (T: %d) seems to be older than what the frame buffer can handle!",
                player.getId(), inputTimestamp);
            return;
        }

        int to = frameIndex + FRAME_BUFFER_SIZE;
        int mid;
        while (from <= to) {
            mid = (from + to) / 2;
            if (frames[mid % FRAME_BUFFER_SIZE].getTimestamp() > inputTimestamp) {
                to = mid - 1;
            } else {
                from = mid + 1;
                newReplayIndex = mid % FRAME_BUFFER_SIZE;
            }
        }
        if (newReplayIndex != frameIndex) {
            frameToReplayFrom =
                frameToReplayFrom < 0 ? newReplayIndex : getLowerIndex(frameToReplayFrom, newReplayIndex);
        }
        frames[newReplayIndex].registerInput(player, input);
    }

    /**
     * Re-simulate a specific frame.
     *
     * @param frameIndex The frame to overwrite.
     */
    private void redoFrame(final int frameIndex) {
        final int lastFrameIndex = frameIndex == 0 ? FRAME_BUFFER_SIZE - 1 : frameIndex - 1;
        final WorldSnapshot lastFrame = frames[lastFrameIndex];
        state.load(lastFrame);
        state.simulate(SECONDS_PER_FRAME, MILLISECONDS_PER_FRAME);
        state.store(frames[frameIndex]);
    }

    /**
     * Simulate a new frame.
     */
    private void doFrame() {
        final WorldSnapshot lastFrame = frames[frameIndex];
        frameIndex = (frameIndex + 1) % FRAME_BUFFER_SIZE;
        state.load(lastFrame);
        state.simulate(SECONDS_PER_FRAME, MILLISECONDS_PER_FRAME);
        state.store(frames[frameIndex]);
    }

    // TODO: Make it less synchronized
    @Synchronized
    public WorldState step() {
        int normalizedTime = (int) (System.currentTimeMillis() - createdAt);

        if (frameToReplayFrom > -1) {
            state.load(frames[frameToReplayFrom]);
            final int frameToStopAt = (frameIndex + 1) % FRAME_BUFFER_SIZE;
            for (int i = (frameToReplayFrom + 1) % FRAME_BUFFER_SIZE;
                 i != frameToStopAt; i = (i + 1) % FRAME_BUFFER_SIZE) {
                redoFrame(i);
            }
            frameToReplayFrom = -1;
            normalizedTime = (int) (System.currentTimeMillis() - createdAt);
        }
        while (frames[frameIndex].getTimestamp() + MILLISECONDS_PER_FRAME <= normalizedTime) {
            int steps = (normalizedTime - frames[frameIndex].getTimestamp()) / MILLISECONDS_PER_FRAME;
            while (--steps >= 0) {
                doFrame();
            }
            normalizedTime = (int) (System.currentTimeMillis() - createdAt);
        }

        return state;
    }
}
