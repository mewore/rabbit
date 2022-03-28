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

class WorldSimulationTest {

    private static WorldState makeWorldState(final AtomicLong frameId) {
        final WorldState state = mock(WorldState.class);
        Mockito.doAnswer(invocation -> frameId.incrementAndGet()).when(state).doStep();
        Mockito.doAnswer(invocation -> {
            final int[] intData = ((WorldSnapshot) invocation.getArgument(0)).getIntData();
            intData[0] = (int) ((frameId.get() >> 30L) & ((1L << 30L) - 1L));
            intData[1] = (int) (frameId.get() & ((1L << 30L) - 1L));
            return null;
        }).when(state).store(any());
        when(state.createEmptySnapshot()).thenAnswer(invocation -> new WorldSnapshot(2, 0));
        when(state.getFrameId()).thenAnswer(invocation -> frameId.get());
        return state;
    }

    @Test
    void testGetCurrentSnapshot() {
        final AtomicLong frameId = new AtomicLong(0);
        final WorldSimulation simulation = new WorldSimulation(makeWorldState(frameId));
        simulation.update(System.currentTimeMillis() + 1000L);
        assertNotEquals(0L, frameId.get());
        final int[] intData = simulation.getCurrentSnapshot().getIntData();
        assertEquals(frameId.get(), ((long) intData[0] << 30L) + intData[1]);
    }

    @Test
    void testGetPastSnapshot() {
        final AtomicLong frameId = new AtomicLong(0);
        final WorldSimulation simulation = new WorldSimulation(makeWorldState(frameId));
        simulation.update(System.currentTimeMillis() + 1000L);
        assertNotEquals(0L, frameId.get());
        final int[] intData = simulation.getPastSnapshot(500).getIntData();
        final long pastSnapshotFrame = ((long) intData[0] << 30L) + intData[1];
        assertTrue(pastSnapshotFrame > 0, "The frame ~500ms after the beginning should be with an ID greater than 0");
        assertTrue(pastSnapshotFrame < frameId.get(),
            "The frame ~500ms after the beginning should be with an ID less than the latest Frame");
    }
}
