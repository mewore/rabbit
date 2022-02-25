package moe.mewore.rabbit.world.editor;

import java.awt.*;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MazeMapCanvasIT {

    private static MazeMapCanvas makeCanvas() {
        final MazeMapCanvas canvas = new MazeMapCanvas();
        canvas.setUp(new FakeMap(), List.of(0), new FakeNoise());
        return canvas;
    }

    @Test
    void testSetNoiseVisibility() {
        makeCanvas().setNoiseVisibility(.3f);
    }

    @Test
    void testSetFertilityVisible() {
        makeCanvas().setFertilityVisible(true);
    }

    @Test
    void testPaint() {
        final Graphics graphics = mock(Graphics.class);
        makeCanvas().paint(graphics);
        verify(graphics, never()).drawImage(any(), anyInt(), anyInt(), any());
    }

    @Test
    void testPaint_uninitialized() {
        final Graphics graphics = mock(Graphics.class);
        new MazeMapCanvas().paint(graphics);
        verify(graphics, never()).drawImage(any(), anyInt(), anyInt(), any());
    }
}
