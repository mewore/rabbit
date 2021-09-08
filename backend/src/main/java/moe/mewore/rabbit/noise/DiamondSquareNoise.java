package moe.mewore.rabbit.noise;

import java.util.Random;

public class DiamondSquareNoise implements Noise {

    private static final int MAX_SAFE_RESOLUTION = 12;

    private final double[][] grid;

    private DiamondSquareNoise(final int resolution) {
        final int count = (1 << resolution) + 1;
        this.grid = new double[count][count];
    }

    /**
     * Create a seamless diamond square noise pattern. It takes up 2^(2R + 3) bytes (2^(2R - 17) MB) for a resolution
     * R. A resolution of 10 results in 8 MB being taken up.
     *
     * @param resolution   The noise resolution.
     * @param random       The random number generator to use for the noise generation.
     * @param zeroAtCenter Whether to make the central point have a value of zero.
     * @return The generated noise.
     */
    public static DiamondSquareNoise createSeamless(final int resolution, final Random random,
        final boolean zeroAtCenter) {
        if (resolution < 0) {
            throw new IllegalArgumentException(
                "A resolution of " + resolution + " is not allowed; the resolution should be non-negative");
        }
        if (resolution > MAX_SAFE_RESOLUTION) {
            throw new IllegalArgumentException(
                "A resolution of " + resolution + " is too large (" + MAX_SAFE_RESOLUTION +
                    " is the largest allowed one)");
        }
        final DiamondSquareNoise result = new DiamondSquareNoise(resolution);
        result.generateSeamless(random, zeroAtCenter);
        return result;
    }

    private static double clamp(final double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    // Sadly, I have no idea how to make this messy code any better.
    @SuppressWarnings("OverlyComplexMethod")
    private void generateSeamless(final Random random, final boolean zeroAtCenter) {
        final int lastIndex = grid.length - 1;
        grid[0][0] = grid[0][lastIndex] = grid[lastIndex][0] = grid[lastIndex][lastIndex] = random.nextDouble();
        int size = grid.length - 1;
        int halfSize = size / 2;
        int subSquares = 1;
        double magnitude = 1.0;

        double topLeft;
        double topRight;
        double center;
        double bottomLeft;
        double bottomRight;

        while (size > 1) {
            // Diamond
            for (int i = 0, minY = 0, maxY = size; i < subSquares; i++, minY += size, maxY += size) {
                for (int j = 0, minX = 0, maxX = size; j < subSquares; j++, minX += size, maxX += size) {
                    grid[minY + halfSize][minX + halfSize] = clamp(
                        (grid[minY][minX] + grid[minY][maxX] + grid[maxY][minX] + grid[maxY][maxX]) * .25 +
                            (random.nextDouble() - 0.5) * magnitude);
                }
            }
            if (subSquares == 1 && zeroAtCenter) {
                grid[halfSize][halfSize] = 0.0;
            }

            // Square
            for (int i = 0, minY = 0, midY = halfSize, maxY = size;
                 i < subSquares; i++, minY += size, midY += size, maxY += size) {
                for (int j = 0, minX = 0, midX = halfSize, maxX = size;
                     j < subSquares; j++, minX += size, midX += size, maxX += size) {
                    topLeft = grid[minY][minX];
                    topRight = grid[minY][maxX];
                    center = grid[midY][midX];
                    bottomLeft = grid[maxY][minX];
                    bottomRight = grid[maxY][maxX];

                    // Top
                    grid[minY][midX] = clamp(
                        (topLeft + topRight + grid[i == 0 ? grid.length - 1 - halfSize : minY - halfSize][midX] +
                            center) * .25 + (random.nextDouble() - 0.5) * magnitude);

                    // Left
                    grid[midY][minX] = clamp(
                        (topLeft + bottomLeft + grid[midY][j == 0 ? grid.length - 1 - halfSize : minX - halfSize] +
                            grid[midY][midX]) * .25 + (random.nextDouble() - 0.5) * magnitude);

                    // Bottom - only if not wrapped
                    if (i < subSquares - 1) {
                        grid[maxY][midX] = clamp(
                            (bottomLeft + center + bottomRight + grid[maxY + halfSize][midX]) * .25 +
                                (random.nextDouble() - 0.5) * magnitude);
                    }

                    // Right - only if not wrapped
                    if (j < subSquares - 1) {
                        grid[midY][maxX] = clamp((topRight + center + bottomRight + grid[midY][maxX + halfSize]) * .25 +
                            (random.nextDouble() - 0.5) * magnitude);
                    }
                }
            }

            // Copy the wrapped values
            for (int i = halfSize; i < grid.length; i += size) {
                grid[grid.length - 1][i] = grid[0][i];
                grid[i][grid.length - 1] = grid[i][0];
            }

            subSquares *= 2;
            size >>= 1;
            halfSize >>= 1;
            magnitude /= 2;
        }

        normalize();
    }

    /**
     * Shift and scale the values so that collectively they range from 0 to 1.
     */
    private void normalize() {
        double minValue = 1;
        double maxValue = 0;
        for (final double[] row : grid) {
            for (final double value : row) {
                maxValue = Math.max(maxValue, value);
                minValue = Math.min(minValue, value);
            }
        }
        final double multiplier = 1 / (maxValue - minValue);
        for (final double[] row : grid) {
            for (int i = 0; i < row.length; i++) {
                row[i] = (row[i] - minValue) * multiplier;
            }
        }
    }

    @Override
    public double get(double x, double y) {
        x = (x % 1.0 + (x < 0 ? 1 : 0)) * (grid.length - 1);
        if (x >= (grid.length - 1)) {
            x = 0;
        }
        y = (y % 1.0 + (y < 0 ? 1 : 0)) * (grid.length - 1);
        if (y >= (grid.length - 1)) {
            y = 0;
        }
        final int xWhole = (int) x;
        final double xFraction = x - (double) xWhole;
        final int yWhole = (int) y;
        final double yFraction = y - (double) yWhole;

        return grid[yWhole][xWhole] * (1.0 - xFraction) * (1.0 - yFraction) +
            grid[yWhole][xWhole + 1] * xFraction * (1.0 - yFraction) +
            grid[yWhole + 1][xWhole] * (1.0 - xFraction) * yFraction +
            grid[yWhole + 1][xWhole + 1] * xFraction * yFraction;
    }
}
