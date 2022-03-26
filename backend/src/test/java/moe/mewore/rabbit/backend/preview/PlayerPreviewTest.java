package moe.mewore.rabbit.backend.preview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPreviewTest {

    @Test
    void testHasMotionLine() {
        final var playerPreview = new PlayerPreview(0, 0, 0, 1, 1, 2, "", "");
        assertTrue(playerPreview.hasMotionLine());
    }

    @Test
    void testHasMotionLine_matchingWithPosition() {
        final var playerPreview = new PlayerPreview(0, 0, 0, 0, 2, 2, "", "");
        assertFalse(playerPreview.hasMotionLine());
    }

    @Test
    void testHasTargetMotionLine() {
        final var playerPreview = new PlayerPreview(0, 0, 1, 1, 2, 1, "", "");
        assertTrue(playerPreview.hasTargetMotionLine());
    }

    @Test
    void testHasTargetMotionLine_matchingWithPosition() {
        final var playerPreview = new PlayerPreview(0, 0, 1, 1, 0, 0, "", "");
        assertFalse(playerPreview.hasTargetMotionLine());
    }

    @Test
    void testHasTargetMotionLine_matchingWithMotionLine() {
        final var playerPreview = new PlayerPreview(0, 0, 1, 1, 1, 1, "", "");
        assertFalse(playerPreview.hasTargetMotionLine());
    }

    @Test
    void testEquals() {
        final var first = new PlayerPreview(1, 1, 1, 1, 1, 1, "", "");
        final var second = new PlayerPreview(1, 1, 1, 1, 1, 1, "", "");
        assertEquals(first, second);
    }

    @Test
    void testEquals_notEqual() {
        final var first = new PlayerPreview(0, 0, 1, 1, 1, 1, "", "");
        final var second = new PlayerPreview(1, 1, 1, 1, 1, 0, "", "");
        assertNotEquals(first, second);
    }

    @Test
    void testHashCode() {
        assertNotEquals(0, new PlayerPreview(0, 0, 1, 1, 1, 1, "", "").hashCode());
    }
}
