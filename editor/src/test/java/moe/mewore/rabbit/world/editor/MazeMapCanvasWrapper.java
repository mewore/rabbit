package moe.mewore.rabbit.world.editor;

import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import lombok.Setter;

import static org.mockito.Mockito.mock;

class MazeMapCanvasWrapper extends MazeMapCanvas {

    private final List<MouseMotionListener> mouseMotionListeners = new ArrayList<>();

    private final List<MouseListener> mouseListeners = new ArrayList<>();

    private final Graphics graphics = mock(Graphics.class);

    @Setter
    private int width = 0;

    @Setter
    private int height = 0;

    public MouseMotionListener getMouseMotionListener() {
        return mouseMotionListeners.get(0);
    }

    public MouseListener getMouseListener() {
        return mouseListeners.get(0);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    public void addMouseListener(final MouseListener listener) {
        mouseListeners.add(listener);
    }

    @Override
    public synchronized MouseListener[] getMouseListeners() {
        return mouseListeners.toArray(new MouseListener[0]);
    }

    @Override
    public void addMouseMotionListener(final MouseMotionListener listener) {
        mouseMotionListeners.add(listener);
    }

    @Override
    public synchronized MouseMotionListener[] getMouseMotionListeners() {
        return mouseMotionListeners.toArray(new MouseMotionListener[0]);
    }
}
