package moe.mewore.rabbit.world;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WorldProperties {

    public static final String FILENAME = "world.properties";

    private static final String SEED_PROPERTY = "seed";

    private static final String COLUMN_COUNT_PROPERTY = "columnCount";

    private static final String ROW_COUNT_PROPERTY = "rowCount";

    private static final String CELL_SIZE_PROPERTY = "cellSize";

    private static final String NOISE_RESOLUTION_PROPERTY = "noiseResolution";

    private static final String NOISE_SHARPNESS_PROPERTY = "noiseSharpness";

    private static final String SMOOTHING_PASSES_PROPERTY = "smoothingPasses";

    private static final String FLIPPED_CELLS_PROPERTY = "flippedCells";

    private final String seed;

    private final int columnCount;

    private final int rowCount;

    private final double cellSize;

    private final double noiseSharpness;

    private final int noiseResolution;

    private final int smoothingPasses;

    private final String flippedCells;

    private WorldProperties(final Properties properties) {
        seed = properties.getProperty(SEED_PROPERTY, "");
        columnCount = Integer.parseInt(properties.getProperty(COLUMN_COUNT_PROPERTY, "1"));
        rowCount = Integer.parseInt(properties.getProperty(ROW_COUNT_PROPERTY, "1"));
        cellSize = Double.parseDouble(properties.getProperty(CELL_SIZE_PROPERTY, "1.0"));
        noiseResolution = Integer.parseInt(properties.getProperty(NOISE_RESOLUTION_PROPERTY, "1"));
        noiseSharpness = Double.parseDouble(properties.getProperty(NOISE_SHARPNESS_PROPERTY, "0.5"));
        smoothingPasses = Integer.parseInt(properties.getProperty(SMOOTHING_PASSES_PROPERTY, "1"));
        flippedCells = properties.getProperty(FLIPPED_CELLS_PROPERTY, "");
    }

    public static WorldProperties getFromClasspath() throws IOException {
        final Properties properties = new Properties();
        try (final InputStream stream = WorldProperties.class.getClassLoader().getResourceAsStream(FILENAME)) {
            properties.load(stream);
        }
        return new WorldProperties(properties);
    }

    public static WorldProperties getFromFile(final File file) throws IOException {
        final Properties properties = new Properties();
        try (final InputStream stream = new FileInputStream(file)) {
            properties.load(stream);
        }
        return new WorldProperties(properties);
    }

    private static long seedStringToLong(final String seed) {
        try {
            return Long.parseLong(seed);
        } catch (final NumberFormatException e) {
            return seed.hashCode();
        }
    }

    public long getSeedAsLong() {
        return seedStringToLong(seed);
    }

    public Set<Integer> getFlippedCellSet() {
        return flippedCells.isBlank()
            ? new HashSet<>()
            : Arrays.stream(flippedCells.split(",")).map(Integer::parseInt).collect(Collectors.toSet());
    }

    private static String makePropertyLine(final String key, final Object value) {
        return key + "=" + value;
    }

    public void save(final File target) throws IOException {
        if (target.createNewFile()) {
            System.out.println(target.getAbsolutePath() + " has been created.");
        }

        try (final FileWriter writer = new FileWriter(target, StandardCharsets.ISO_8859_1)) {
            writer.write(asString());
        }
    }

    String asString() {
        final List<String> lines = new ArrayList<>();
        lines.add(makePropertyLine(SEED_PROPERTY, seed));
        lines.add(makePropertyLine(COLUMN_COUNT_PROPERTY, columnCount));
        lines.add(makePropertyLine(ROW_COUNT_PROPERTY, rowCount));
        lines.add(makePropertyLine(CELL_SIZE_PROPERTY, cellSize));
        lines.add(makePropertyLine(NOISE_RESOLUTION_PROPERTY, noiseResolution));
        lines.add(makePropertyLine(NOISE_SHARPNESS_PROPERTY, noiseSharpness));
        lines.add(makePropertyLine(SMOOTHING_PASSES_PROPERTY, smoothingPasses));
        lines.add(makePropertyLine(FLIPPED_CELLS_PROPERTY, flippedCells));
        return String.join("\n", lines) + "\n";
    }
}
