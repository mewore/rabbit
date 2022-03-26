package moe.mewore.rabbit.backend.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WorldSnapshotTest {

    @Test
    void testWorldSnapshot() {
        final WorldSnapshot snapshot = new WorldSnapshot(5, 6);
        assertArrayEquals(new int[5], snapshot.getIntData());
        assertArrayEquals(new float[6], snapshot.getFloatData());
    }
}
