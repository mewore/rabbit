package moe.mewore.rabbit.entities.world.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Setter;
import moe.mewore.rabbit.entities.world.MazeMap;
import moe.mewore.rabbit.noise.Noise;

class MazeMapCanvas extends Canvas {

    private static final int PIXELS_PER_CELL = 20;

    private static final int MAX_IMAGE_PIXELS = 10000000;

    private int offsetX = 0;

    private int offsetY = 0;

    @Setter
    private @Nullable Noise noise;

    @Setter
    private float noiseVisibility = 0f;

    private @Nullable MazeMap map;

    private @Nullable MouseEvent lastMouseDrag;

    private @Nullable BufferedImage image;

    public MazeMapCanvas() {
        super();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseMotionListener(new CanvasMouseMotionListener());
        addMouseListener(new CanvasMouseListener());
    }

    public void setMap(final MazeMap map) {
        this.map = map;
        image = null;
    }

    @Override
    public void paint(final Graphics g) {
        if (map == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        if (image == null) {
            image = generateImage(map);
        }
        renderImage(image, g);
    }

    private BufferedImage generateImage(final @NonNull MazeMap map) {
        int imageWidth = PIXELS_PER_CELL * map.getWidth();
        int imageHeight = PIXELS_PER_CELL * map.getHeight();
        if (imageWidth * imageHeight > MAX_IMAGE_PIXELS) {
            final double overhead = (double) (imageWidth * imageHeight) / MAX_IMAGE_PIXELS;
            imageWidth = (int) (imageWidth / Math.sqrt(overhead));
            imageHeight = (int) (imageHeight / Math.sqrt(overhead));
        }
        final BufferedImage newImage = map.render(imageWidth, imageHeight);
        if (noise != null) {
            final float[] a = new float[3];
            for (int i = 0; i < imageHeight; i++) {
                for (int j = 0; j < imageWidth; j++) {
                    final Color color = new Color(newImage.getRGB(j, i));
                    color.getRGBColorComponents(a);
                    final float noiseValue = (float) noise.get((double) j / imageWidth, (double) i / imageHeight);
                    float intensity = Math.abs((noiseValue - 0.5f) * 2) * noiseVisibility;
                    intensity *= intensity;
                    if (noiseValue > 0.5) {
                        for (int c = 0; c < 3; c++) {
                            a[c] = a[c] * (1 - intensity) + intensity;
                        }
                    } else {
                        for (int c = 0; c < 3; c++) {
                            a[c] = a[c] * (1 - intensity);
                        }
                    }
                    newImage.setRGB(j, i, new Color(a[0], a[1], a[2]).getRGB());
                }
            }
        }
        return newImage;
    }

    private void renderImage(final BufferedImage image, final Graphics g) {
        while (offsetX < 0) {
            offsetX += image.getWidth();
        }
        final int minX = offsetX - (offsetX / image.getWidth() + 1) * image.getWidth();
        while (offsetY < 0) {
            offsetY += image.getHeight();
        }
        final int minY = offsetY - (offsetY / image.getHeight() + 1) * image.getHeight();
        for (int y = minY; y < getHeight(); y += image.getHeight()) {
            for (int x = minX; x < getWidth(); x += image.getWidth()) {
                g.drawImage(image, x, y, this);
            }
        }
    }

    private class CanvasMouseMotionListener implements MouseMotionListener {

        @Override
        public void mouseDragged(final MouseEvent e) {
            if (lastMouseDrag != null) {
                offsetX += e.getX() - lastMouseDrag.getX();
                offsetY += e.getY() - lastMouseDrag.getY();
                paint(getGraphics());
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            lastMouseDrag = e;
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
        }
    }

    private class CanvasMouseListener implements MouseListener {

        @Override
        public void mouseClicked(final MouseEvent e) {
            lastMouseDrag = e;
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            lastMouseDrag = e;
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lastMouseDrag = null;
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
        }

        @Override
        public void mouseExited(final MouseEvent e) {
        }
    }
}
