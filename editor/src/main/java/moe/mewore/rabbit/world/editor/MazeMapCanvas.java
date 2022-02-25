package moe.mewore.rabbit.world.editor;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.Setter;
import moe.mewore.rabbit.noise.Noise;
import moe.mewore.rabbit.world.MazeMap;

/**
 * A map that is particularly for the rendering of a {@link MazeMap}.
 */
class MazeMapCanvas extends Canvas {

    private static final int PIXELS_PER_CELL = 30;

    private static final int MAX_IMAGE_PIXELS = 10000000;

    private int offsetX = 0;

    private int offsetY = 0;

    private final Set<Integer> paintedCells = new HashSet<>();

    @Getter
    @Setter
    private Set<Integer> flippedCells = new HashSet<>();

    private boolean fertilityVisible = false;

    private @Nullable MapCompositeImage imageData;

    private float noiseVisibility = 0f;

    private @Nullable Boolean currentPaint = null;

    private @Nullable Integer hoveredCell = null;

    private @Nullable MouseEvent lastMouseDrag;

    public void setNoiseVisibility(final float noiseVisibility) {
        if (noiseVisibility == this.noiseVisibility) {
            return;
        }
        this.noiseVisibility = noiseVisibility;
        if (imageData != null) {
            imageData.setNoiseVisibility(noiseVisibility);
            imageData.redrawNoise();
            imageData.updateUiIndicators(paintedCells, currentPaint, hoveredCell);
            paint(getGraphics());
        }
    }

    public void setFertilityVisible(final boolean fertilityVisible) {
        if (fertilityVisible == this.fertilityVisible) {
            return;
        }
        this.fertilityVisible = fertilityVisible;
        if (imageData != null) {
            imageData.setFertilityVisible(fertilityVisible);
            imageData.drawFromScratch(flippedCells);
            paint(getGraphics());
        }
    }

    public MazeMapCanvas() {
        super();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseMotionListener(new CanvasMouseMotionListener());
        addMouseListener(new CanvasMouseListener());
    }

    public void setUp(final MazeMap map, final Collection<Integer> flippedCells, final Noise noise) {
        int imageWidth = PIXELS_PER_CELL * map.getWidth();
        int imageHeight = PIXELS_PER_CELL * map.getHeight();
        if (imageWidth * imageHeight > MAX_IMAGE_PIXELS) {
            final double overhead = (double) (imageWidth * imageHeight) / MAX_IMAGE_PIXELS;
            imageWidth = (int) (imageWidth / Math.sqrt(overhead));
            imageHeight = (int) (imageHeight / Math.sqrt(overhead));
        }
        this.flippedCells.clear();
        this.flippedCells.addAll(flippedCells);
        imageData = new MapCompositeImage(imageWidth, imageHeight, map, noise);

        offsetX = offsetX % imageWidth;
        offsetY = offsetY % imageHeight;

        imageData.setNoiseVisibility(noiseVisibility);
        imageData.setFertilityVisible(fertilityVisible);
        imageData.drawFromScratch(flippedCells);
        imageData.updateUiIndicators(paintedCells, currentPaint, hoveredCell);
    }

    @Override
    public void paint(final Graphics graphics) {
        if (imageData == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        imageData.updateUiIndicators(paintedCells, currentPaint, hoveredCell);
        final int minX = offsetX - (offsetX / imageData.getImageWidth() + 1) * imageData.getImageWidth();
        final int minY = offsetY - (offsetY / imageData.getImageHeight() + 1) * imageData.getImageHeight();
        for (int y = minY; y < getHeight(); y += imageData.getImageHeight()) {
            for (int x = minX; x < getWidth(); x += imageData.getImageWidth()) {
                graphics.drawImage(imageData.getImageWithUiIndicators(), x, y, this);
            }
        }
    }

    private int getHoveredCell(final MouseEvent e, final MapCompositeImage data) {
        return data.getHoveredCell(e.getX() - getX() - offsetX, e.getY() - getY() - offsetY);
    }

    private class CanvasMouseMotionListener implements MouseMotionListener {

        @Override
        public void mouseDragged(final MouseEvent e) {
            if (imageData == null) {
                lastMouseDrag = null;
                return;
            }
            if (lastMouseDrag != null) {
                offsetX += e.getX() - lastMouseDrag.getX();
                while (offsetX < 0) {
                    offsetX += imageData.getImageWidth();
                }
                offsetX = offsetX % imageData.getImageWidth();
                offsetY += e.getY() - lastMouseDrag.getY();
                while (offsetY < 0) {
                    offsetY += imageData.getImageHeight();
                }
                offsetY = offsetY % imageData.getImageHeight();
                paint(getGraphics());
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                lastMouseDrag = e;
            } else if (currentPaint != null) {
                final MazeMap map = imageData.getMap();
                final int newHoveredCell = getHoveredCell(e, imageData);
                if (hoveredCell == null || hoveredCell != newHoveredCell) {
                    hoveredCell = newHoveredCell;
                    if (map.getCell(hoveredCell / map.getWidth(), hoveredCell % map.getWidth()) != currentPaint) {
                        paintedCells.add(hoveredCell);
                    }
                    imageData.updateUiIndicators(paintedCells, currentPaint, hoveredCell);
                    paint(getGraphics());
                }
            }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            if (imageData == null || lastMouseDrag != null) {
                hoveredCell = null;
                return;
            }

            final int newHoveredCell = getHoveredCell(e, imageData);
            if (hoveredCell == null || hoveredCell != newHoveredCell) {
                hoveredCell = newHoveredCell;
                paint(getGraphics());
            }
        }
    }

    private class CanvasMouseListener implements MouseListener {

        private static final int LEFT_MOUSE_BUTTON = 1;

        private static final int RIGHT_MOUSE_BUTTON = 3;

        @Override
        public void mouseClicked(final MouseEvent e) {
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            currentPaint = null;
            lastMouseDrag = null;
            if (e.getButton() == LEFT_MOUSE_BUTTON && imageData != null) {
                final int hoveredCell = getHoveredCell(e, imageData);
                final MazeMap map = imageData.getMap();
                currentPaint = !map.getCell(hoveredCell / map.getWidth(), hoveredCell % map.getWidth());
                paintedCells.add(hoveredCell);
                paint(getGraphics());
            } else if (e.getButton() == RIGHT_MOUSE_BUTTON) {
                lastMouseDrag = e;
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (imageData != null && currentPaint != null && !paintedCells.isEmpty()) {
                final MazeMap map = imageData.getMap();
                for (final int cell : paintedCells) {
                    map.setCell(cell / map.getWidth(), cell % map.getWidth(), currentPaint);
                    if (flippedCells.contains(cell)) {
                        flippedCells.remove(cell);
                    } else {
                        flippedCells.add(cell);
                    }
                }
                map.recomputeWalls();
                imageData.redrawFlippedCells(paintedCells, flippedCells);
                paintedCells.clear();
                imageData.updateUiIndicators(paintedCells, currentPaint, hoveredCell);
                paint(getGraphics());
            }
            currentPaint = null;
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
