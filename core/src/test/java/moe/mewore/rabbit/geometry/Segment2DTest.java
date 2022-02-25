package moe.mewore.rabbit.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Segment2DTest {

    @Test
    void testDistanceToPointSquared() {
        final Line2D line = new Segment2D(new Vector2(0, 1), new Vector2(1, 3));
        assertEquals(10.0, line.distanceToPointSquared(new Vector2(-1, -2)), 1e-6);
    }
}
