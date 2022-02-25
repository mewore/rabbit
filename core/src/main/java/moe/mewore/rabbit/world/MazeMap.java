package moe.mewore.rabbit.world;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.geometry.ConvexPolygon;
import moe.mewore.rabbit.geometry.Vector2;
import moe.mewore.rabbit.noise.Noise;

// I'm too retarded to make an algorithmic/geometric class which isn't complex.
@SuppressWarnings({"OverlyComplexMethod", "OverlyComplexClass"})
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MazeMap extends BinaryEntity {

    private static final int[] dx = {-1, 1, 0, 0, -1, -1, 1, 1};

    private static final int[] dy = {0, 0, -1, 1, -1, 1, -1, 1};

    private static final Color SOLID_CENTER = new Color(60, 60, 60);

    private static final Color WALKABLE_CENTER = new Color((125 << 24) | (25 << 16) | (150 << 8) | 100);

    private static final Color SOLID_COLOR = new Color(100, 100, 110);

    private static final double SMOOTHING = 0.3;

    @Getter
    private final int width;

    @Getter
    private final int height;

    private final boolean[][] map;

    private final List<MazeWall> walls;

    private final int[][][] relevantPolygonIndices;

    public static MazeMap createSeamless(final int width, final int height, final Random random,
        final int clearOutPasses, final Noise opennessNoise, final Set<Integer> flippedCells) {
        final boolean[][] map = createSeamlessLabyrinth(width, height, random);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                map[Math.min(Math.max(height / 2 + i, 0), height - 1)][Math.min(Math.max(width / 2 + j, 0),
                    width - 1)] = true;
            }
        }
        final boolean[][] oldMap = new boolean[height][width];
        for (int i = 0; i < height; i++) {
            System.arraycopy(map[i], 0, oldMap[i], 0, map[i].length);
        }
        for (int pass = 0; pass < clearOutPasses; pass++) {
            for (int i = 0; i < height; i++) {
                System.arraycopy(map[i], 0, oldMap[i], 0, map[i].length);
            }
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    final double openness = opennessNoise.get((double) (j) / width, (double) (i) / height);
                    if (map[i][j] == openness > 0.5) {
                        continue;
                    }
                    final double baseFlipChance = Math.abs(openness - 0.5) * 2;
                    int neighbouringWalls = 0;
                    for (int d = 0; d < 8; d++) {
                        if (!oldMap[wrap(i + dy[d], height)][wrap(j + dx[d], width)]) {
                            neighbouringWalls += Math.abs(dy[d]) + Math.abs(dx[d]);
                        }
                    }

                    final double flipChance = Math.pow(baseFlipChance,
                        map[i][j] ? (12 - neighbouringWalls) : neighbouringWalls);

                    if (random.nextDouble() < flipChance) {
                        map[i][j] = !map[i][j];
                    }
                }
            }
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j]) {
                    // Smooth walls out
                    int neighbouringWalls = 0;
                    for (int d = 0; d < 4; d++) {
                        if (!oldMap[wrap(i + dy[d], height)][wrap(j + dx[d], width)]) {
                            neighbouringWalls++;
                        }
                    }
                    if (neighbouringWalls >= 3) {
                        map[i][j] = false;
                    }
                } else if ((oldMap[wrap(i - 1, height)][wrap(j - 1, width)] &&
                    oldMap[wrap(i + 1, height)][wrap(j + 1, width)]) ||
                    oldMap[wrap(i - 1, height)][wrap(j + 1, width)] &&
                        oldMap[wrap(i + 1, height)][wrap(j - 1, width)]) {
                    // Add a small diagonal passage
                    int neighbouringWalls = 0;
                    for (int d = 0; d < 8; d++) {
                        if (!oldMap[wrap(i + dy[d], height)][wrap(j + dx[d], width)]) {
                            neighbouringWalls++;
                        }
                    }
                    if (neighbouringWalls >= 5) {
                        map[i][j] = true;
                    }
                }
            }
        }


        final @Nullable CellTraversal[][] traversals = traverse(map, height / 2, width / 2);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                map[i][j] = traversals[i][j] != null;
                if (flippedCells.contains(i * width + j)) {
                    map[i][j] = !map[i][j];
                }
            }
        }

        final int[][][] relevantPolygonIndices = new int[height][width][];
        final List<MazeWall> walls = generateWallsFromMap(width, height, map, relevantPolygonIndices);
        return new MazeMap(width, height, map, walls, relevantPolygonIndices);
    }

    private static List<MazeWall> generateWallsFromMap(final int width, final int height, final boolean[][] map,
        final int[][][] relevantPolygonIndices) {
        final int[][] rowSums = new int[height][width];
        for (int i = 0; i < height; i++) {
            rowSums[i][0] = map[i][0] ? 0 : 1;
            for (int j = 1; j < width; j++) {
                rowSums[i][j] = rowSums[i][j - 1] + (map[i][j] ? 0 : 1);
            }
        }

        final List<List<List<Integer>>> relevantPolygonIndexLists = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            relevantPolygonIndexLists.add(new ArrayList<>());
            for (int j = 0; j < width; j++) {
                relevantPolygonIndexLists.get(i).add(new ArrayList<>());
            }
        }

        final boolean[][] occupiedByPolygons = new boolean[height][width];
        final List<MazeWall> walls = new ArrayList<>();

        int right;
        int bottom;
        double leftX;
        double rightX;
        double topY;
        double bottomY;

        int diagonalRow;
        int diagonalCol;
        for (int top = 0; top < height; top++) {
            for (int left = 0; left < width; left++) {
                if (map[top][left]) {
                    topY = top;
                    leftX = left;
                    bottomY = top + 1;
                    rightX = left + 1;
                    for (int d = 4; d < 8; d++) {
                        diagonalRow = wrap(top + dy[d], height);
                        diagonalCol = wrap(left + dx[d], width);
                        if (map[diagonalRow][diagonalCol] || map[diagonalRow][left] || map[top][diagonalCol]) {
                            continue;
                        }
                        final List<Vector2> points = new ArrayList<>(3);
                        final Vector2 corner = new Vector2((dx[d] < 0 ? leftX : rightX) / width,
                            (dy[d] < 0 ? topY : bottomY) / height);
                        points.add(corner);
                        final double deltaX = (dx[d] < 0 ? SMOOTHING : -SMOOTHING) / width;
                        final double deltaY = (dy[d] < 0 ? SMOOTHING : -SMOOTHING) / height;
                        if (dx[d] * dy[d] > 0) {
                            // Top-left or bottom-right
                            points.add(corner.plus(0, deltaY));
                            points.add(corner.plus(deltaX, 0));
                        } else {
                            // Top-right or bottom-left
                            points.add(corner.plus(deltaX, 0));
                            points.add(corner.plus(0, deltaY));
                        }
                        final int polygonIndex = walls.size();
                        walls.add(new MazeWall(top, left, top, left, new ConvexPolygon(points)));

                        for (int i = top - 1; i <= top + 1; i++) {
                            for (int j = left - 1; j <= left + 1; j++) {
                                relevantPolygonIndexLists.get(wrap(i, height)).get(wrap(j, width)).add(polygonIndex);
                            }
                        }
                    }
                    continue;
                }
                if (occupiedByPolygons[top][left]) {
                    continue;
                }

                right = left;
                while (right + 1 < width && !map[top][right + 1] && !occupiedByPolygons[top][right + 1]) {
                    right++;
                }
                bottom = top;
                while (bottom + 1 < height &&
                    rowSums[bottom + 1][right] - (left == 0 ? 0 : rowSums[bottom + 1][left - 1]) == right - left + 1) {
                    bottom++;
                }
                for (int i = top; i <= bottom; i++) {
                    for (int j = left; j <= right; j++) {
                        occupiedByPolygons[i][j] = true;
                    }
                }
                leftX = left;
                rightX = right + 1;
                topY = top;
                bottomY = bottom + 1;
                final List<Vector2> points = new ArrayList<>(4);
                if (map[bottom][wrap(left - 1, width)] && map[wrap(bottom + 1, height)][left]) {
                    points.add(new Vector2(leftX / width, (bottomY - SMOOTHING) / height));
                    points.add(new Vector2((leftX + SMOOTHING) / width, bottomY / height));
                } else {
                    points.add(new Vector2(leftX / width, bottomY / height));
                }
                if (map[bottom][wrap(right + 1, width)] && map[wrap(bottom + 1, height)][right]) {
                    points.add(new Vector2((rightX - SMOOTHING) / width, bottomY / height));
                    points.add(new Vector2(rightX / width, (bottomY - SMOOTHING) / height));
                } else {
                    points.add(new Vector2(rightX / width, bottomY / height));
                }
                if (map[top][wrap(right + 1, width)] && map[wrap(top - 1, height)][right]) {
                    points.add(new Vector2(rightX / width, (topY + SMOOTHING) / height));
                    points.add(new Vector2((rightX - SMOOTHING) / width, topY / height));
                } else {
                    points.add(new Vector2(rightX / width, topY / height));
                }
                if (map[top][wrap(left - 1, width)] && map[wrap(top - 1, height)][left]) {
                    points.add(new Vector2((leftX + SMOOTHING) / width, topY / height));
                    points.add(new Vector2(leftX / width, (topY + SMOOTHING) / height));
                } else {
                    points.add(new Vector2(leftX / width, topY / height));
                }
                final int polygonIndex = walls.size();
                walls.add(new MazeWall(top, left, bottom, right, new ConvexPolygon(points)));

                for (int i = top - 1; i <= bottom + 1; i++) {
                    for (int j = left - 1; j <= right + 1; j++) {
                        relevantPolygonIndexLists.get(wrap(i, height)).get(wrap(j, width)).add(polygonIndex);
                    }
                }
            }
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                relevantPolygonIndices[i][j] = relevantPolygonIndexLists.get(i)
                    .get(j)
                    .stream()
                    .mapToInt(a -> a)
                    .toArray();
            }
        }

        return walls;
    }

    private static boolean[][] createSeamlessLabyrinth(final int width, final int height, final Random random) {
        final boolean[][] map = new boolean[height][width];
        final boolean[][] tried = new boolean[height][width];

        final Stack<Integer> cellsToExplore = new Stack<>();
        final int firstRow = random.nextInt(height);
        final int firstCol = random.nextInt(width);
        cellsToExplore.add(firstRow * height + firstCol);
        map[firstRow][firstCol] = true;
        tried[firstRow][firstCol] = true;

        final int[] dIndices = {0, 1, 2, 3};
        while (!cellsToExplore.isEmpty()) {
            final int currentCell = cellsToExplore.pop();
            final int row = currentCell / width;
            final int col = currentCell % width;
            shuffle(dIndices, random);
            for (int i = 0; i < 4; i++) {
                final int nextRow = wrap(row + dy[dIndices[i]], height);
                final int nextCol = wrap(col + dx[dIndices[i]], width);
                if (!tried[nextRow][nextCol]) {
                    boolean isValid = true;
                    for (int j = 0; j < 8; j++) {
                        final int otherRow = wrap(nextRow + dy[j], height);
                        final int otherCol = wrap(nextCol + dx[j], width);
                        if (map[otherRow][otherCol] && (Math.abs(otherRow - row) + Math.abs(otherCol - col) >= 2)) {
                            isValid = false;
                            break;
                        }
                    }
                    if (isValid) {
                        map[nextRow][nextCol] = true;
                        cellsToExplore.add(nextRow * width + nextCol);
                    }
                    tried[nextRow][nextCol] = true;
                }
            }
        }
        return map;
    }

    private static void shuffle(final int[] values, final Random random) {
        int index;
        for (int i = 1; i < values.length; i++) {
            index = random.nextInt(i + 1);
            if (index < i) {
                // Swap [i] and [index] with bitwise XOR
                values[i] ^= values[index];
                values[index] ^= values[i];
                values[i] ^= values[index];
            }
        }
    }

    private static int wrap(final int coordinate, final int size) {
        return coordinate < 0 ? coordinate + size : coordinate % size;
    }

    private static @Nullable CellTraversal[][] traverse(final boolean[][] map, final int fromRow, final int fromCol) {
        final int height = map.length;
        final int width = map[0].length;
        final @Nullable CellTraversal[][] result = new CellTraversal[height][width];
        if (!map[fromRow][fromCol]) {
            return result;
        }

        final boolean[][] explored = new boolean[height][width];
        result[fromRow][fromCol] = new CellTraversal(0, -1, -1);
        final Queue<Integer> toExplore = new PriorityQueue<>(
            Comparator.comparingDouble(index -> result[index / width][index % width].getMinDistance()));
        toExplore.add(fromRow * width + fromCol);
        while (!toExplore.isEmpty()) {
            final int row = toExplore.peek() / width;
            final int col = toExplore.poll() % width;
            if (explored[row][col]) {
                continue;
            }
            explored[row][col] = true;
            final double distance = result[row][col].getMinDistance();

            for (int d = 0; d < 8; d++) {
                final int newRow = wrap(row + dy[d], height);
                final int newCol = wrap(col + dx[d], width);
                final double newDistance = distance + Math.sqrt(dx[d] * dx[d] + dy[d] * dy[d]);

                if (!explored[newRow][newCol] && map[newRow][newCol] &&
                    (result[newRow][newCol] == null || result[newRow][newCol].getMinDistance() > newDistance)) {
                    result[newRow][newCol] = new CellTraversal(newDistance, dx[d], dy[d]);
                    toExplore.add(newRow * width + newCol);
                }
            }
        }

        return result;
    }

    public boolean getCell(final int row, final int col) {
        return map[wrap(row, height)][wrap(col, width)];
    }

    public void setCell(final int row, final int col, final boolean value) {
        map[wrap(row, height)][wrap(col, width)] = value;
    }

    public void recomputeWalls() {
        final List<MazeWall> walls = generateWallsFromMap(width, height, map, relevantPolygonIndices);
        this.walls.clear();
        this.walls.addAll(walls);
    }

    public double get(double x, double y) {
        x = (x + 1.0) % 1.0;
        y = (y + 1.0) % 1.0;
        final int row = Math.min((int) (y * height), height);
        final int col = Math.min((int) (x * width), width);
        final int[] wallIndices = relevantPolygonIndices[row][col];
        for (final int wallIndex : wallIndices) {
            if (walls.get(wallIndex).getPolygon().containsPoint(new Vector2(x, y))) {
                return -1.0;
            }
        }

        final double requiredDistance = .8 / Math.max(height, width);
        final double requiredDistanceSquared = requiredDistance * requiredDistance;
        final double[] offsetsX = Stream.of(0, col == 0 ? 1 : -10, (col == width - 1) ? -1 : -10)
            .filter(value -> value > -10)
            .mapToDouble(a -> a)
            .toArray();
        final double[] offsetsY = Stream.of(0, row == 0 ? 1 : -10, (row == height - 1) ? -1 : -10)
            .filter(value -> value > -10)
            .mapToDouble(a -> a)
            .toArray();

        double minDistanceSquared = requiredDistanceSquared + 1.0;
        for (final double offsetX : offsetsX) {
            for (final double offsetY : offsetsY) {
                final Vector2 point = new Vector2(x + offsetX, y + offsetY);
                for (final int wallIndex : wallIndices) {
                    minDistanceSquared = Math.min(minDistanceSquared,
                        walls.get(wallIndex).getPolygon().distanceToPointSquared(point));
                }
            }
        }
        if (minDistanceSquared > requiredDistanceSquared) {
            return 0.0;
        }

        final double maxFertility = 1.0 - Math.sqrt(minDistanceSquared / requiredDistanceSquared);
        return maxFertility * maxFertility;
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(height);
        output.writeInt(width);
        for (final boolean[] row : map) {
            for (final boolean value : row) {
                output.writeBoolean(value);
            }
        }
        appendCollectionToBinaryOutput(walls, output);
    }

    public void applyFertilityToImage(final BufferedImage image, final int fromX, final int fromY, final int toX,
        final int toY) {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();

        if (Math.max((double) (imageHeight) / height, (double) (imageWidth) / width) <= 15.0) {
            return;
        }

        final float[] walkableWithTrees = new float[3];
        new Color(50, 200, 100).getRGBColorComponents(walkableWithTrees);
        final float[] walkable = new float[3];
        Color.WHITE.getRGBColorComponents(walkable);
        final float[] currentColor = new float[3];
        final double xStep = 1.0 / imageWidth;
        final double yStep = 1.0 / imageHeight;
        double normalizedY = fromY * yStep;
        double normalizedX;
        double fertility;
        int trueY;
        int trueX;
        for (int y = fromY; y <= toY; y++, normalizedY += yStep) {
            normalizedX = fromX * xStep;
            trueY = wrap(y, imageHeight);
            for (int x = fromX; x <= toX; x++, normalizedX += xStep) {
                trueX = wrap(x, imageWidth);
                if (image.getRGB(trueX, trueY) == -1) {
                    fertility = get(normalizedX, normalizedY);
                    if (fertility < 0) {
                        image.setRGB(trueX, trueY, SOLID_CENTER.getRGB());
                    } else {
                        for (int c = 0; c < 3; c++) {
                            currentColor[c] = (float) (walkableWithTrees[c] * fertility +
                                walkable[c] * (1.0 - fertility));
                        }
                        image.setRGB(trueX, trueY,
                            new Color(currentColor[0], currentColor[1], currentColor[2]).getRGB());
                    }
                }
            }
        }
    }

    private List<Polygon> getPolygonsToDraw(final int imageWidth, final int imageHeight) {
        return walls.stream()
            .map(MazeWall::getPolygon)
            .map(polygon -> new Polygon(
                polygon.getPoints().stream().mapToInt(point -> (int) (Math.round(point.getX() * imageWidth))).toArray(),
                polygon.getPoints()
                    .stream()
                    .mapToInt(point -> (int) (Math.round(point.getY() * imageHeight)))
                    .toArray(), polygon.getPoints().size()))
            .collect(Collectors.toUnmodifiableList());
    }

    public void render(final BufferedImage image) {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        System.out.printf("Rendering a %dx%d map with %d polygons into a %dx%d px image...%n", width, height,
            walls.size(), imageWidth, imageHeight);

        final List<Polygon> polygonsToDraw = getPolygonsToDraw(imageWidth, imageHeight);

        final Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, imageWidth, imageHeight);

        graphics.setColor(SOLID_COLOR);
        polygonsToDraw.forEach(graphics::fillPolygon);

        applyFertilityToImage(image, 0, 0, imageWidth - 1, imageHeight - 1);

        applyEverythingElseToImage(image, polygonsToDraw);
    }

    public void applyEverythingElseToImage(final BufferedImage image) {
        applyEverythingElseToImage(image, getPolygonsToDraw(image.getWidth(), image.getHeight()));
    }

    private void applyEverythingElseToImage(final BufferedImage image, final List<Polygon> polygonsToDraw) {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        System.out.printf("Rendering a %dx%d map with %d polygons into a %dx%d px image...%n", width, height,
            walls.size(), imageWidth, imageHeight);

        final Graphics2D graphics = image.createGraphics();
        graphics.setColor(SOLID_COLOR);
        polygonsToDraw.forEach(graphics::fillPolygon);

        graphics.addRenderingHints(
            new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

        graphics.setColor(SOLID_CENTER);
        graphics.setStroke(new BasicStroke(Math.min((imageHeight * .1f) / height, (imageWidth * .1f) / width)));
        polygonsToDraw.forEach(graphics::drawPolygon);

        final double centerHorizontalSize = .25 * imageWidth / (width * 2);
        final double centerVerticalSize = .25 * imageHeight / (height * 2);
        final double centerYStep = (double) (imageHeight) / height;
        final double centerXStep = (double) (imageWidth) / width;
        double centerY = 0.5 * centerYStep;
        for (int i = 0; i < height; i++, centerY += centerYStep) {
            double centerX = 0.5 * centerXStep;
            for (int j = 0; j < map[i].length; j++, centerX += centerXStep) {
                graphics.setColor(map[i][j] ? WALKABLE_CENTER : SOLID_CENTER);
                graphics.fillPolygon(new int[]{(int) (centerX - centerHorizontalSize), (int) centerX, (int) (centerX +
                        centerHorizontalSize), (int) centerX},
                    new int[]{(int) centerY, (int) (centerY + centerVerticalSize), (int) centerY, (int) (centerY -
                        centerVerticalSize)}, 4);
            }
        }

        graphics.setStroke(new BasicStroke((float) (centerYStep * .2)));

        double maxDistance = -1;
        final @Nullable CellTraversal[][] traversals = traverse(map, height / 2, width / 2);
        for (final CellTraversal[] row : traversals) {
            for (final CellTraversal traversal : row) {
                if (traversal != null) {
                    maxDistance = Math.max(maxDistance, traversal.getMinDistance());
                }
            }
        }
        centerY = 0.5 * centerYStep;
        for (int i = 0; i < traversals.length; i++, centerY += centerYStep) {
            double centerX = 0.5 * centerXStep;
            for (int j = 0; j < traversals[i].length; j++, centerX += centerXStep) {
                if (traversals[i][j] == null || traversals[i][j].getMinDistance() < 0) {
                    continue;
                }

                final double distanceRatio = traversals[i][j].getMinDistance() / maxDistance;
                final double extremity = Math.abs((distanceRatio - 0.5) * 2);
                graphics.setColor(distanceRatio > 0.5
                    ? new Color(125 + (int) (125 * extremity), 120 - (int) (120 * extremity), 0)
                    : new Color(125 - (int) (125 * extremity), 120 + (int) (50 * extremity), (int) (70 * extremity)));
                graphics.drawLine((int) (centerX), (int) (centerY), (int) (centerX - centerXStep * traversals[i][j].getDx()),
                    (int) (centerY - centerYStep * traversals[i][j].getDy()));

                if (i - traversals[i][j].getDy() < 0 || i - traversals[i][j].getDy() >= height ||
                    j - traversals[i][j].getDx() < 0 || j - traversals[i][j].getDx() >= width) {
                    final double otherCenterX = (wrap(j - traversals[i][j].getDx(), width) + 0.5) * centerXStep;
                    final double otherCenterY = (wrap(i - traversals[i][j].getDy(), height) + 0.5) * centerYStep;
                    graphics.drawLine((int) (otherCenterX), (int) (otherCenterY),
                        (int) (otherCenterX + centerXStep * traversals[i][j].getDx()),
                        (int) (otherCenterY + centerYStep * traversals[i][j].getDy()));
                }

            }
        }
    }

    public BufferedImage render(final int imageWidth, final int imageHeight) {
        final BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        render(image);
        return image;
    }

    public void render(final int imageWidth, final int imageHeight, final String name) {
        final BufferedImage image = render(imageWidth, imageHeight);
        try {
            ImageIO.write(image, "png", new File(name + ".png"));
        } catch (final IOException e) {
            System.out.println("Error while creating maze preview: " + e);
        }
    }
}
