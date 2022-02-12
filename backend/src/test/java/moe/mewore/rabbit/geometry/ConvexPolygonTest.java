package moe.mewore.rabbit.geometry;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvexPolygonTest {

    private static ConvexPolygon makePolygon() {
        return new ConvexPolygon(
            Arrays.asList(new Vector2(0, 0), new Vector2(5, 0), new Vector2(7, 3), new Vector2(3, 6),
                new Vector2(-2, 4)));
    }

    @Test
    void testIncludesPoint() {
        final ConvexPolygon polygon = makePolygon();
        assertTrue(polygon.includesPoint(new Vector2(1, 3)));
        assertFalse(polygon.includesPoint(new Vector2(1, -1)));
    }

    @Test
    void testIncludesPoint_withOffset() {
        final ConvexPolygon polygon = makePolygon().withOffset(new Vector2(0, 10));
        assertFalse(polygon.includesPoint(new Vector2(1, 3)));
    }

    @Test
    void testDistanceToPointSquared() {
        final ConvexPolygon polygon = makePolygon();
        assertEquals(9.0, polygon.distanceToPointSquared(new Vector2(1, -3)), 1e-6);
    }

    @Test
    void testSolveForPoint() {
        final Line2D line = new Line2D(new Vector2(0, 1), new Vector2(1, 3));
        assertEquals(3.0, line.solveForPoint(new Vector2(-1, 2)), 1e-6);
    }
}
