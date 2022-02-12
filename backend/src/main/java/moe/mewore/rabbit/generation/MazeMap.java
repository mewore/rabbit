package moe.mewore.rabbit.generation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.geometry.ConvexPolygon;
import moe.mewore.rabbit.geometry.Vector2;

// I'm too retarded to make an algorithmic/geometric class which isn't complex.
@SuppressWarnings({"OverlyComplexMethod", "OverlyComplexClass"})
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MazeMap extends BinaryEntity {

    private static final int[] dx = {-1, 1, 0, 0};

    private static final int[] dy = {0, 0, -1, 1};

    private static final double SMOOTHING = 0.3;

    private static final double MAX_CLEAR_OUT_CHANCE = 0.3;

    private final boolean[][] map;

    private final List<ConvexPolygon> polygons;

    public static MazeMap createSeamless(final int width, final int height, final Random random,
        final int clearOutPasses) {
        final boolean[][] map = createSeamlessLabyrinth(width, height, random);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                map[height / 2 + i][width / 2 + j] = true;
            }
        }
        final boolean[][] oldMap = new boolean[height][width];
        for (int i = 0; i < height; i++) {
            System.arraycopy(map, 0, oldMap, 0, map[i].length);
        }
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j]) {
                    int neighbouringWalls = 0;
                    for (int d = 0; d < 4; d++) {
                        if (!oldMap[wrap(i + dy[d], height)][wrap(j + dx[d], width)]) {
                            neighbouringWalls++;
                        }
                    }
                    if (neighbouringWalls >= 3) {
                        map[i][j] = false;
                    }
                }
            }
        }
        for (int pass = 0; pass < clearOutPasses; pass++) {
            for (int i = 0; i < height; i++) {
                System.arraycopy(map, 0, oldMap, 0, map[i].length);
            }
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (map[i][j]) {
                        continue;
                    }
                    int neighbouringWalls = 0;
                    for (int d = 0; d < 4; d++) {
                        if (!oldMap[wrap(i + dy[d], height)][wrap(j + dx[d], width)]) {
                            neighbouringWalls++;
                        }
                    }
                    if (neighbouringWalls < 4) {
                        int neighbouringDiagonalWalls = 0;
                        for (int dx = -1; dx <= 1; dx += 2) {
                            for (int dy = -1; dy <= 1; dy += 2) {
                                if (!oldMap[wrap(i + dy, height)][wrap(j + dx, width)]) {
                                    neighbouringDiagonalWalls++;
                                }
                            }
                        }
                        final double chanceToClearOut = neighbouringWalls + neighbouringDiagonalWalls == 0
                            ? 0.0
                            : Math.pow(MAX_CLEAR_OUT_CHANCE, neighbouringDiagonalWalls);
                        if (random.nextDouble() < chanceToClearOut) {
                            map[i][j] = true;
                        }
                    }
                }
            }
        }

        final List<ConvexPolygon> polygons = generatePolygonsFromMap(width, height, map);
        return new MazeMap(map, polygons);
    }

    private static List<ConvexPolygon> generatePolygonsFromMap(final int width, final int height,
        final boolean[][] map) {
        final int[][] wallSums = new int[height][width];
        wallSums[0][0] = map[0][0] ? 0 : 1;
        for (int i = 1; i < height; i++) {
            wallSums[i][0] = wallSums[i - 1][0] + (map[i][0] ? 0 : 1);
        }
        for (int j = 1; j < width; j++) {
            wallSums[0][j] = wallSums[0][j - 1] + (map[0][j] ? 0 : 1);
        }
        for (int i = 1; i < height; i++) {
            for (int j = 1; j < width; j++) {
                wallSums[i][j] = wallSums[i][j - 1] + wallSums[i - 1][j] - wallSums[i - 1][j - 1] + (map[i][j] ? 0 : 1);
            }
        }

        final boolean[][] occupiedByPolygons = new boolean[height][width];
        final List<ConvexPolygon> polygons = new ArrayList<>();

        int right;
        int bottom;
        double leftX;
        double rightX;
        double topY;
        double bottomY;
        for (int top = 0; top < height; top++) {
            for (int left = 0; left < width; left++) {
                if (map[top][left] || occupiedByPolygons[top][left]) {
                    continue;
                }

                right = left;
                while (right + 1 < width && !map[top][right + 1] && !occupiedByPolygons[top][right + 1]) {
                    right++;
                }
                bottom = top;
                while (bottom + 1 < height && wallSums[bottom + 1][right] == wallSums[bottom][right]) {
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
                if (map[bottom][wrap(left - 1, map[0].length)] && map[wrap(bottom + 1, map.length)][left]) {
                    points.add(new Vector2(leftX / width, (bottomY - SMOOTHING) / height));
                    points.add(new Vector2((leftX + SMOOTHING) / width, bottomY / height));
                } else {
                    points.add(new Vector2(leftX / width, bottomY / height));
                }
                if (map[bottom][wrap(right + 1, map[0].length)] && map[wrap(bottom + 1, map.length)][right]) {
                    points.add(new Vector2((rightX - SMOOTHING) / width, bottomY / height));
                    points.add(new Vector2(rightX / width, (bottomY - SMOOTHING) / height));
                } else {
                    points.add(new Vector2(rightX / width, bottomY / height));
                }
                if (map[top][wrap(right + 1, map[0].length)] && map[wrap(top - 1, map.length)][right]) {
                    points.add(new Vector2(rightX / width, (topY + SMOOTHING) / height));
                    points.add(new Vector2((rightX - SMOOTHING) / width, topY / height));
                } else {
                    points.add(new Vector2(rightX / width, topY / height));
                }
                if (map[top][wrap(left - 1, map[0].length)] && map[wrap(top - 1, map.length)][left]) {
                    points.add(new Vector2((leftX + SMOOTHING) / width, topY / height));
                    points.add(new Vector2(leftX / width, (topY + SMOOTHING) / height));
                } else {
                    points.add(new Vector2(leftX / width, topY / height));
                }
                final ConvexPolygon polygon = new ConvexPolygon(points);
                polygons.add(polygon);
            }
        }
        return polygons;
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
                    for (int j = 0; j < 4; j++) {
                        final int otherRow = wrap(nextRow + dy[j], height);
                        final int otherCol = wrap(nextCol + dx[j], width);
                        if (map[otherRow][otherCol] && (otherRow != row || otherCol != col)) {
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

    public static void main(final String[] args) {
        final MazeMap maze = MazeMap.createSeamless(30, 30, new Random(11L), Integer.parseInt(args[0]));
        maze.generatePreview(1024, 1024);
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

    public double get(final double x, final double y) {
        for (final ConvexPolygon polygon : polygons) {
            if (polygon.includesPoint(new Vector2(x, y))) {
                return -1.0;
            }
        }
        final double requiredDistance = 0.3 / Math.max(map.length, map[0].length);
        final double requiredDistanceSquared = requiredDistance * requiredDistance;
        double maxFertility = 0.0;
        for (final double offsetX : new double[]{0, -1, 1}) {
            for (final double offsetY : new double[]{0, -1, 1}) {
                final Vector2 point = new Vector2(x + offsetX, y + offsetY);
                for (final ConvexPolygon polygon : polygons) {
                    maxFertility = Math.max(maxFertility,
                        requiredDistanceSquared - polygon.distanceToPointSquared(point));
                }
            }
        }
        return maxFertility / requiredDistanceSquared;
    }

    public void generatePreview(final int width, final int height) {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        final int walkable = (50 << 16) | (200 << 8) | 100;
        final int solid = (255 << 24) | (200 << 16) | (100 << 8);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final double fertility = get((double) (x) / width, (double) (y) / height);
                final int p = fertility < 0 ? solid : (((int) (fertility * 255) << 24) | walkable);
                img.setRGB(x, y, p);
            }
        }

        try {
            ImageIO.write(img, "png", new File("maze.png"));
        } catch (final IOException e) {
            System.out.println("Error while creating maze preview: " + e);
        }
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(map.length);
        output.writeInt(map[0].length);
        for (final boolean[] row : map) {
            for (final boolean value : row) {
                output.writeBoolean(value);
            }
        }
        appendCollectionToBinaryOutput(polygons, output);
    }
}
