package moe.mewore.rabbit.world.editor;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import moe.mewore.rabbit.world.MazeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MazeMapCanvasTest {

    private static final int LEFT_MOUSE_BUTTON = 1;

    private static final int RIGHT_MOUSE_BUTTON = 3;

    private MazeMapCanvasWrapper canvas;

    @BeforeEach
    void setUp() {
        canvas = new MazeMapCanvasWrapper();
    }

    @Test
    void testSetNoiseVisibility() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        canvas.setNoiseVisibility(.3f);
    }

    @Test
    void testSetNoiseVisibility_uninitialized() {
        canvas.setNoiseVisibility(.3f);
    }

    @Test
    void testSetNoiseVisibility_same() {
        canvas.setNoiseVisibility(0f);
    }

    @Test
    void testSetUp() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        assertEquals(1, canvas.getMouseMotionListeners().length);
        assertEquals(1, canvas.getMouseListeners().length);
    }

    @Test
    void testSetUp_flippedCells() {
        final Set<Integer> originalFlipped = Stream.of(1).collect(Collectors.toSet());
        canvas.setFlippedCells(originalFlipped);
        canvas.setUp(new FakeMap(), List.of(0), new FakeNoise());

        assertEquals(Set.of(0), canvas.getFlippedCells());
        assertSame(originalFlipped, canvas.getFlippedCells());
    }

    @Test
    void testSetUp_huge() {
        final var map = mock(MazeMap.class);
        when(map.getColumnCount()).thenReturn(10000);
        when(map.getRowCount()).thenReturn(20000);

        canvas.setUp(map, java.util.List.of(0), new FakeNoise());
        canvas.setWidth(20);
        canvas.setHeight(2);
        canvas.paint(canvas.getGraphics());
        final var imageArgumentCaptor = ArgumentCaptor.forClass(BufferedImage.class);
        verify(canvas.getGraphics()).drawImage(imageArgumentCaptor.capture(), anyInt(), anyInt(), same(canvas));
        assertEquals(2236, imageArgumentCaptor.getValue().getWidth());
        assertEquals(4472, imageArgumentCaptor.getValue().getHeight());
    }

    @Test
    void testPaint() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        canvas.paint(canvas.getGraphics());
    }

    @Test
    void testPaint_uninitialized() {
        canvas.paint(canvas.getGraphics());
        verify(canvas.getGraphics(), never()).drawImage(any(), anyInt(), anyInt(), any());
    }

    @Test
    void testSetFertilityVisible() {
        canvas.setFertilityVisible(true);
    }

    @Test
    void testSetFertilityVisible_uninitialized() {
        canvas.setFertilityVisible(true);
    }

    @Test
    void testSetFertilityVisible_same() {
        canvas.setFertilityVisible(false);
    }

    @Test
    void testMouseDragged() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        canvas.getMouseMotionListener().mouseDragged(makeMouseEvent(0, 0, LEFT_MOUSE_BUTTON));
    }

    @Test
    void testMouseMoved() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        canvas.getMouseMotionListener().mouseMoved(makeMouseEvent(0, 0, LEFT_MOUSE_BUTTON));
    }

    @Test
    void testPanning() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        canvas.getMouseListener().mousePressed(makeMouseEvent(0, 0, RIGHT_MOUSE_BUTTON));
        canvas.getMouseMotionListener().mouseDragged(makeMouseEvent(-10, -10, RIGHT_MOUSE_BUTTON));
        canvas.getMouseListener().mouseReleased(makeMouseEvent(-10, -10, RIGHT_MOUSE_BUTTON));
    }

    @Test
    void testPanning_triedToInterrupt() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        canvas.getMouseListener().mousePressed(makeMouseEvent(0, 0, RIGHT_MOUSE_BUTTON));
        canvas.getMouseListener().mousePressed(makeMouseEvent(10, 10, LEFT_MOUSE_BUTTON));
        canvas.getMouseListener().mouseReleased(makeMouseEvent(10, 10, LEFT_MOUSE_BUTTON));
    }

    @Test
    void testPainting() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        canvas.getMouseListener().mousePressed(makeMouseEvent(0, 0, LEFT_MOUSE_BUTTON));
        canvas.getMouseMotionListener().mouseDragged(makeMouseEvent(10, 10, LEFT_MOUSE_BUTTON));
        canvas.getMouseMotionListener().mouseMoved(makeMouseEvent(10, 10, LEFT_MOUSE_BUTTON));
        canvas.getMouseListener().mouseReleased(makeMouseEvent(10, 10, LEFT_MOUSE_BUTTON));
        assertEquals(Set.of(0), canvas.getFlippedCells());
    }

    @Test
    void testPainting_erase() {
        canvas.setUp(new FakeMap(), List.of(0), new FakeNoise());
        canvas.getMouseListener().mousePressed(makeMouseEvent(0, 0, LEFT_MOUSE_BUTTON));
        canvas.getMouseMotionListener().mouseDragged(makeMouseEvent(0, 0, LEFT_MOUSE_BUTTON));
        canvas.getMouseListener().mouseReleased(makeMouseEvent(10, 10, LEFT_MOUSE_BUTTON));
        assertEquals(Set.of(), canvas.getFlippedCells());
    }

    @Test
    void testPainting_triedToInterrupt() {
        canvas.setUp(new FakeMap(), List.of(), new FakeNoise());
        canvas.getMouseListener().mousePressed(makeMouseEvent(0, 0, LEFT_MOUSE_BUTTON));
        canvas.getMouseListener().mousePressed(makeMouseEvent(10, 10, RIGHT_MOUSE_BUTTON));
        canvas.getMouseListener().mouseReleased(makeMouseEvent(10, 10, RIGHT_MOUSE_BUTTON));
        canvas.getMouseMotionListener().mouseDragged(makeMouseEvent(10, 10, LEFT_MOUSE_BUTTON));
        canvas.getMouseListener().mouseReleased(makeMouseEvent(10, 10, LEFT_MOUSE_BUTTON));
        assertEquals(Set.of(0), canvas.getFlippedCells());
    }

    private MouseEvent makeMouseEvent(final int x, final int y, final int button) {
        return new MouseEvent(canvas, 0, 0, 0, x, y, 0, false, button);
    }
}
