package moe.mewore.rabbit.world.editor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.Setter;
import moe.mewore.rabbit.noise.Noise;
import moe.mewore.rabbit.world.MazeMap;

/**
 * A combination of a {@link MazeMap} and some layers:
 * 1. Fertility layer - if viewing fertility is enabled, it contains gradients showing the plant abundance in areas
 * of the map; if no fertility is shown, then it is just blank. It is the most expensive to update because of its
 * per-pixel sampling, but on the other hand only regions of it need to be updated if small parts of the map have
 * been changed.
 * 2. Base layer - contains the previous layer and polygons of the map, the shortest paths and the centers of the
 * cells. The additions are fairly cheap to make.
 * 3. Noise layer - if noise is set to be visible, then this layer is a combination of the base layer and the noise
 * that the map has been created with. If it is set to be invisible, it is just skipped.
 * 4. UI layer - the last layer, which is a combination of the previous ones and is the only one the user needs to
 * see. In addition to the content from the other layers, it also contains the squares signifying how and where the
 * user is interacting with the map (e.g., over which cell the cursor is hovering).
 */
public class MapCompositeImage {

    private static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;

    private static final int INFINITY = 1000000000;

    private static final float STROKE_PER_CELL = 0.1f;

    @Getter
    private final int imageWidth;

    @Getter
    private final int imageHeight;

    @Getter
    private final MazeMap map;

    private final Noise noise;

    private final BufferedImage fertilityImage;

    private final BufferedImage baseMapImage;

    private final BufferedImage imageWithNoise;

    private final BasicStroke stroke;

    private final int lineThickness;

    @Getter
    private final BufferedImage imageWithUiIndicators;

    private int uiIndicatorClearMinX = INFINITY;

    private int uiIndicatorClearMaxX = -INFINITY;

    private int uiIndicatorClearMinY = INFINITY;

    private int uiIndicatorClearMaxY = -INFINITY;

    @Setter
    private float noiseVisibility = 0f;

    @Setter
    private boolean fertilityVisible = false;

    public MapCompositeImage(final int imageWidth, final int imageHeight, final MazeMap map, final Noise noise) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.map = map;
        this.noise = noise;
        this.stroke = new BasicStroke(STROKE_PER_CELL *
            Math.min((float) imageWidth / map.getColumnCount(), (float) imageHeight / map.getRowCount()));
        lineThickness = (int) Math.ceil(stroke.getLineWidth());

        fertilityImage = new BufferedImage(imageWidth, imageHeight, IMAGE_TYPE);
        baseMapImage = new BufferedImage(imageWidth, imageHeight, IMAGE_TYPE);
        imageWithNoise = new BufferedImage(imageWidth, imageHeight, IMAGE_TYPE);
        imageWithUiIndicators = new BufferedImage(imageWidth, imageHeight, IMAGE_TYPE);
    }

    private static void addNoiseToImage(final BufferedImage image, final Noise noise, final float noiseVisibility) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final float[] a = new float[3];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                final Color color = new Color(image.getRGB(j, i));
                color.getRGBColorComponents(a);
                final float noiseValue = (float) noise.get((double) j / width, (double) i / height);
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
                image.setRGB(j, i, new Color(a[0], a[1], a[2]).getRGB());
            }
        }
    }

    /**
     * Assuming both images are with the same dimensions, make {@code target} exactly the same as {@code source}.
     *
     * @param source The source image.
     * @param target The target image.
     */
    private static void cloneImage(final BufferedImage source, final BufferedImage target) {
        final Graphics graphics = target.createGraphics();
        graphics.clearRect(0, 0, target.getWidth(), target.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
    }

    private static void cloneImagePartially(final BufferedImage source, final Graphics targetGraphics, final int leftX,
        final int topY, final int rightX, final int bottomY) {
        targetGraphics.drawImage(source, leftX, topY, rightX, bottomY, leftX, topY, rightX, bottomY, null);
    }

    /**
     * Draw, assuming nothing else has been drawn yet.
     *
     * @param flippedCells The indices of the currently flipped cells.
     */
    public void drawFromScratch(final Collection<Integer> flippedCells) {
        redrawRegion(0, 0, imageWidth, imageHeight, flippedCells);
    }

    /**
     * Some cells have just been flipped. Redraw the area around these cells, hopefully skipping unnecessary updating of
     * faraway spots.
     *
     * @param flippedCells    The cells that have just been flipped.
     * @param allFlippedCells All currently flipped cells.
     */
    public void redrawFlippedCells(final Collection<Integer> flippedCells, final Collection<Integer> allFlippedCells) {
        int minX = imageWidth;
        int maxX = 0;
        int minY = imageHeight;
        int maxY = 0;

        for (final int cell : flippedCells) {
            final CellRenderInfo cellRenderInfo = new CellRenderInfo(cell, map.getColumnCount(), map.getRowCount(),
                imageWidth, imageHeight);
            minX = Math.min(minX, cellRenderInfo.getX() - cellRenderInfo.getWidth());
            maxX = Math.max(maxX, cellRenderInfo.getX() + cellRenderInfo.getWidth() * 2 - 1);
            minY = Math.min(minY, cellRenderInfo.getY() - cellRenderInfo.getHeight());
            maxY = Math.max(maxY, cellRenderInfo.getY() + cellRenderInfo.getHeight() * 2 - 1);
        }

        redrawRegion(minX, minY, maxX, maxY, allFlippedCells);
    }

    /**
     * If the fertility layer is enabled, redraw it only in a specific region because it's very computationally
     * expensive to draw it everywhere. The rest of the layers are drawn completely. The UI layer is NOT drawn.
     *
     * @param minX            The left bound of the region.
     * @param minY            The upper bound of the region.
     * @param maxX            The right bound of the region (inclusive).
     * @param maxY            The lower bound of the region (inclusive).
     * @param allFlippedCells A collection of the indices of all cells which have been flipped for this map.
     */
    public void redrawRegion(final int minX, final int minY, final int maxX, final int maxY,
        final Collection<Integer> allFlippedCells) {

        // Base map image
        if (fertilityVisible) {
            final Graphics2D graphics2D = fertilityImage.createGraphics();
            graphics2D.setColor(Color.WHITE);
            graphics2D.fillRect(minX, minY, maxX - minX + 1, maxY - minY + 1);
            graphics2D.dispose();
            map.applyFertilityToImage(fertilityImage, minX, minY, maxX, maxY);
            cloneImage(fertilityImage, baseMapImage);
        } else {
            final Graphics2D graphics2D = baseMapImage.createGraphics();
            graphics2D.setColor(Color.WHITE);
            graphics2D.fillRect(0, 0, imageWidth, imageHeight);
            graphics2D.dispose();
        }
        map.applyEverythingElseToImage(baseMapImage);

        // Then, the flipped map cell indicators are added on top
        final List<CellRenderInfo> flippedCellRenderInfo = allFlippedCells.stream()
            .map(this::getCellRenderInfo)
            .collect(Collectors.toUnmodifiableList());
        addFlippedIndicatorsToImage(baseMapImage, flippedCellRenderInfo);

        // The image with noise is a cloned one so that the noise visibility can be changed without rendering the whole
        // map again
        if (noiseVisibility > 0f) {
            cloneImage(baseMapImage, imageWithNoise);
            addNoiseToImage(imageWithNoise, noise, noiseVisibility);
        }

        cloneImage(noiseVisibility > 0f ? imageWithNoise : baseMapImage, imageWithUiIndicators);
    }

    /**
     * Redraw the noise layer if it is visible. IT is he layer containing both the map and its noise.
     */
    public void redrawNoise() {
        if (noiseVisibility > 0f) {
            cloneImage(baseMapImage, imageWithNoise);
            addNoiseToImage(imageWithNoise, noise, noiseVisibility);
            cloneImage(imageWithNoise, imageWithUiIndicators);
        } else {
            cloneImage(baseMapImage, imageWithUiIndicators);
        }
    }

    /**
     * Update the topmost image layer containing the indicators of where the user is hovering and painting/interacting.
     *
     * @param paintedCells The indices of the cells which are being painted by the user. The user must still be
     *                     holding a mouse button down with which he has painted over those cells.
     * @param currentPaint The paint that is currently being applied to the cells, if any.
     * @param hoveredCell  The index of the cell over which the user is hovering at the moment.
     */
    public void updateUiIndicators(final @Nullable Collection<Integer> paintedCells,
        final @Nullable Boolean currentPaint, final @Nullable Integer hoveredCell) {

        clearUiIndicators();

        final Graphics2D graphics = imageWithUiIndicators.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (paintedCells != null && currentPaint != null) {
            final List<CellRenderInfo> paintedCellRenderInfo = paintedCells.stream()
                .map(this::getCellRenderInfo)
                .collect(Collectors.toUnmodifiableList());
            final float value = currentPaint ? 1f : 0f;
            graphics.setColor(new Color(value, value, value, 0.5f));
            for (final CellRenderInfo paintedCell : paintedCellRenderInfo) {
                graphics.fillRect(paintedCell.getX(), paintedCell.getY(), paintedCell.getWidth(),
                    paintedCell.getHeight());
                updateUiIndicatorClearBounds(paintedCell);
            }
        }

        if (hoveredCell != null) {
            drawHoveredCell(this.getCellRenderInfo(hoveredCell), graphics);
        }

        graphics.dispose();
    }

    private void clearUiIndicators() {
        if (uiIndicatorClearMinX <= uiIndicatorClearMaxX && uiIndicatorClearMinY <= uiIndicatorClearMaxY) {
            final Graphics graphics = imageWithUiIndicators.createGraphics();
            final BufferedImage source = noiseVisibility > 0f ? imageWithNoise : baseMapImage;

            cloneImagePartially(source, graphics, uiIndicatorClearMinX, uiIndicatorClearMinY, uiIndicatorClearMaxX,
                uiIndicatorClearMaxY);
            cloneImagePartially(source, graphics, 0, 0, lineThickness, imageHeight);
            cloneImagePartially(source, graphics, 0, 0, imageWidth, lineThickness);
            cloneImagePartially(source, graphics, imageWidth - lineThickness, 0, imageWidth, imageHeight);
            cloneImagePartially(source, graphics, 0, imageHeight - lineThickness, imageWidth, imageHeight);
            graphics.dispose();
        }
        uiIndicatorClearMinX = uiIndicatorClearMinY = INFINITY;
        uiIndicatorClearMaxX = uiIndicatorClearMaxY = -INFINITY;
    }

    private void drawHoveredCell(final CellRenderInfo hoveredCell, final Graphics2D graphics) {
        graphics.setColor(new Color(0.3f, 0.5f, 1.0f, 0.5f));
        graphics.fillRect(hoveredCell.getX(), hoveredCell.getY(), hoveredCell.getWidth(), hoveredCell.getHeight());

        graphics.setColor(Color.BLUE);
        graphics.setStroke(stroke);
        for (int dy = -imageHeight; dy <= imageHeight; dy += imageHeight) {
            for (int dx = -imageWidth; dx <= imageWidth; dx += imageWidth) {
                graphics.drawRect(hoveredCell.getX() + dx, hoveredCell.getY() + dy, hoveredCell.getWidth(),
                    hoveredCell.getHeight());
            }
        }
        updateUiIndicatorClearBounds(hoveredCell);
    }

    private void updateUiIndicatorClearBounds(final CellRenderInfo paintedCell) {
        uiIndicatorClearMinX = Math.min(uiIndicatorClearMinX, paintedCell.getX() - lineThickness);
        uiIndicatorClearMinY = Math.min(uiIndicatorClearMinY, paintedCell.getY() - lineThickness);
        uiIndicatorClearMaxX = Math.max(uiIndicatorClearMaxX,
            paintedCell.getX() + paintedCell.getWidth() + lineThickness);
        uiIndicatorClearMaxY = Math.max(uiIndicatorClearMaxY,
            paintedCell.getY() + paintedCell.getHeight() + lineThickness);

    }

    private void addFlippedIndicatorsToImage(final BufferedImage image,
        final List<CellRenderInfo> flippedCellRenderInfo) {
        final Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(new Color(0.75f, 1f, 0.25f, 0.2f));
        for (final CellRenderInfo flippedCell : flippedCellRenderInfo) {
            graphics.fillOval(flippedCell.getX(), flippedCell.getY(), flippedCell.getWidth(), flippedCell.getHeight());
        }
        graphics.setColor(new Color(0.15f, 0.25f, 1f, 1f));
        graphics.setStroke(stroke);
        for (final CellRenderInfo flippedCell : flippedCellRenderInfo) {
            graphics.drawOval(flippedCell.getX(), flippedCell.getY(), flippedCell.getWidth(), flippedCell.getHeight());
        }

        graphics.dispose();
    }

    /**
     * The index (starting from 0 from the top-left, left-to-right, top-to-bottom) of the cell which is at the (x,y)
     * coordinate/pixel on the map image.
     *
     * @param x The X coordinate of the pixel.
     * @param y The Y coordinate of the pixel.
     * @return The cell index.
     */
    public int getHoveredCell(final int x, final int y) {
        final int col = (((x + imageWidth) % imageWidth) * map.getColumnCount()) / imageWidth;
        final int row = (((y + imageHeight) % imageHeight) * map.getRowCount()) / imageHeight;
        return row * map.getColumnCount() + col;
    }

    private CellRenderInfo getCellRenderInfo(final int cell) {
        return new CellRenderInfo(cell, map.getColumnCount(), map.getRowCount(), imageWidth, imageHeight);
    }
}
