package moe.mewore.rabbit.backend.simulation;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealtimeSimulationTest {

    private static RabbitWorldState makeWorldState(final AtomicLong frameId) {
        final RabbitWorldState state = mock(RabbitWorldState.class);
        when(state.getMaxPlayerCount()).thenReturn(0);
        Mockito.doAnswer(invocation -> frameId.incrementAndGet()).when(state).doStep();
        Mockito.doAnswer(invocation -> {
            final byte[] frame = invocation.getArgument(0);
            assert frameId.get() < 100;
            frame[0] = (byte) frameId.get();
            return null;
        }).when(state).store(any());
        when(state.getFrameSize()).thenReturn(1);
        when(state.getFrameId()).thenAnswer(invocation -> frameId.get());
        return state;
    }

    @Test
    void testGetCurrentSnapshot() {
        final AtomicLong frameId = new AtomicLong(0);
        final RealtimeSimulation simulation = new RealtimeSimulation(makeWorldState(frameId));
        simulation.update(System.currentTimeMillis() + 1000L);
        assertNotEquals(0L, frameId.get());
        final byte[] frame = simulation.getCurrentSnapshot();
        assertEquals(frameId.get(), frame[0]);
    }

    @Test
    void testGetPastSnapshot() {
        final AtomicLong frameId = new AtomicLong(0);
        final RealtimeSimulation simulation = new RealtimeSimulation(makeWorldState(frameId));
        simulation.update(System.currentTimeMillis() + 1000L);
        assertNotEquals(0L, frameId.get());
        final byte[] frame = simulation.getPastSnapshot(500);
        final long pastSnapshotFrame = frame[0];
        assertTrue(pastSnapshotFrame > 0, "The frame ~500ms after the beginning should be with an ID greater than 0");
        assertTrue(pastSnapshotFrame < frameId.get(),
            "The frame ~500ms after the beginning should be with an ID less than the latest Frame");
    }
}
