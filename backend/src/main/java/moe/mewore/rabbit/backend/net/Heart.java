package moe.mewore.rabbit.backend.net;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Heart {

    public static final int DEFAULT_LATENCY = 100;

    private static final int DEFAULT_DELAY = DEFAULT_LATENCY * 2;

    private static final long MAXIMUM_DELAY = TimeUnit.MINUTES.toMillis(1);

    private static final int BEAT_HISTORY_SIZE = 10;

    private static final int DELAY_TO_LATENCY_DIVISOR = BEAT_HISTORY_SIZE * 2;

    private final int[] delays = makeDefaultDelayArray();

    private final long[] sentAt = new long[BEAT_HISTORY_SIZE];

    private final Consumer<Integer> latencyConsumer;

    private final int[] expectedHeartbeatId = new int[BEAT_HISTORY_SIZE];

    private int delaySum = DEFAULT_DELAY * BEAT_HISTORY_SIZE;

    private int currentHeartbeatId = 0;

    private int currentHeartbeatIndex = BEAT_HISTORY_SIZE - 1;

    static int[] makeDefaultDelayArray() {
        final int[] result = new int[BEAT_HISTORY_SIZE];
        Arrays.fill(result, DEFAULT_DELAY);
        return result;
    }

    public @Nullable Integer prepareBeat() {
        final long now = System.currentTimeMillis();
        for (int i = 0; i < BEAT_HISTORY_SIZE; i++) {
            currentHeartbeatIndex = (currentHeartbeatIndex + 1) % BEAT_HISTORY_SIZE;
            if (expectedHeartbeatId[currentHeartbeatIndex] <= 0 ||
                now - sentAt[currentHeartbeatIndex] > MAXIMUM_DELAY) {
                expectedHeartbeatId[currentHeartbeatIndex] = ++currentHeartbeatId;
                sentAt[currentHeartbeatIndex] = now;
                return currentHeartbeatId;
            }
        }
        return null;
    }

    public void receive(final int heartbeatId) {
        for (int i = 0; i < BEAT_HISTORY_SIZE; i++) {
            if (expectedHeartbeatId[i] == heartbeatId) {
                delaySum -= delays[i];
                delaySum += delays[i] = (int) (System.currentTimeMillis() - sentAt[i]);
                latencyConsumer.accept(delaySum / DELAY_TO_LATENCY_DIVISOR);
                expectedHeartbeatId[i] = -1;
                sentAt[i] = -1;
                return;
            }
        }
    }
}
