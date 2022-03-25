package moe.mewore.rabbit.backend.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class WorldSnapshotTest {

    @Test
    void testCopy() {
        final WorldSnapshot previousSnapshot = new WorldSnapshot(5, 5);
        previousSnapshot.getIntData()[0] = 1;
        previousSnapshot.getFloatData()[0] = 1f;

        final WorldSnapshot snapshot = new WorldSnapshot(5, 5);
        snapshot.copy(previousSnapshot);

        assertNotEquals(previousSnapshot.getIntData(), snapshot.getIntData());
        assertNotEquals(previousSnapshot.getFloatData(), snapshot.getFloatData());
        assertNotSame(previousSnapshot.getIntData(), snapshot.getIntData());
        assertNotSame(previousSnapshot.getFloatData(), snapshot.getFloatData());
    }
}
