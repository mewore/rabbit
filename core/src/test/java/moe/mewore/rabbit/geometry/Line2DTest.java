package moe.mewore.rabbit.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Line2DTest {

    @Test
    void testDistanceToPointSquared() {
        final Line2D line = new Line2D(new Vector2(0, 1), new Vector2(1, 3));
        assertEquals(1.8, line.distanceToPointSquared(new Vector2(-1, 2)), 1e-6);

        final Line2D xAxis = new Line2D(new Vector2(0, 0), new Vector2(1, 0));
        assertEquals(0.0, xAxis.distanceToPointSquared(new Vector2(-2, 0)), 1e-6);
    }

    @Test
    void testSolveForPoint() {
        final Line2D line = new Line2D(new Vector2(0, 1), new Vector2(1, 3));
        assertEquals(3.0, line.solveForPoint(new Vector2(-1, 2)), 1e-6);

        final Line2D yAxis = new Line2D(new Vector2(0, 0), new Vector2(0, 1));
        assertEquals(0.0, yAxis.solveForPoint(new Vector2(0, 2)), 1e-6);
    }
}
