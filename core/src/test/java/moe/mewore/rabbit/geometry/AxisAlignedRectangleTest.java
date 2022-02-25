package moe.mewore.rabbit.geometry;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AxisAlignedRectangleTest {

    private static AxisAlignedRectangle makeRectangle() {
        return new AxisAlignedRectangle(
            Arrays.asList(new Vector2(0, 0), new Vector2(5, 0), new Vector2(7, 3), new Vector2(3, 6),
                new Vector2(-2, 4)));
    }

    @Test
    void testContainsPoint() {
        final AxisAlignedRectangle rectangle = makeRectangle();
        assertTrue(rectangle.containsPoint(new Vector2(-1, 1)));
        assertFalse(rectangle.containsPoint(new Vector2(1, -1)));
    }
}
