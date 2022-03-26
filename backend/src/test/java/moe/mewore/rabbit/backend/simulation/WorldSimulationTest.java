package moe.mewore.rabbit.backend.simulation;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldSimulationTest {

    private static WorldState makeWorldState(final AtomicInteger frameId) {
        final WorldState state = mock(WorldState.class);
        Mockito.doAnswer(invocation -> frameId.incrementAndGet()).when(state).doStep();
        Mockito.doAnswer(invocation -> ((WorldSnapshot) invocation.getArgument(0)).getIntData()[0] = frameId.get())
            .when(state)
            .store(any());
        when(state.createEmptySnapshot()).thenAnswer(invocation -> new WorldSnapshot(1, 0));
        when(state.getFrameId()).thenAnswer(invocation -> frameId.get());
        return state;
    }

    @Test
    void testGetCurrentSnapshot() {
        final AtomicInteger frameId = new AtomicInteger(0);
        final WorldSimulation simulation = new WorldSimulation(makeWorldState(frameId));
        simulation.update(System.currentTimeMillis() + 1000L);
        assertNotEquals(0, frameId.get());
        assertEquals(frameId.get(), simulation.getCurrentSnapshot().getIntData()[0]);
    }

    @Test
    void testGetPastSnapshot() {
        final AtomicInteger frameId = new AtomicInteger(0);
        final WorldSimulation simulation = new WorldSimulation(makeWorldState(frameId));
        simulation.update(System.currentTimeMillis() + 1000L);
        assertNotEquals(0, frameId.get());
        final int pastSnapshotFrame = simulation.getPastSnapshot(500).getIntData()[0];
        assertTrue(pastSnapshotFrame > 0, "The frame ~500ms after the beginning should be with an ID greater than 0");
        assertTrue(pastSnapshotFrame < frameId.get(),
            "The frame ~500ms after the beginning should be with an ID less than the latest Frame");
    }
}
