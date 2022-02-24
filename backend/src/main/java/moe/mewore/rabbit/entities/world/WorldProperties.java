package moe.mewore.rabbit.entities.world;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import moe.mewore.rabbit.Server;

@Getter
@RequiredArgsConstructor
public class WorldProperties {

    public static final String FILENAME = "world.properties";

    private static final String SEED_PROPERTY = "seed";

    private static final String WIDTH_PROPERTY = "width";

    private static final String HEIGHT_PROPERTY = "height";

    private static final String NOISE_RESOLUTION_PROPERTY = "noiseResolution";

    private static final String NOISE_SHARPNESS_PROPERTY = "noiseSharpness";

    private static final String SMOOTHING_PASSES_PROPERTY = "smoothingPasses";

    private static final String FLIPPED_CELLS_PROPERTY = "flippedCells";

    private final String seed;

    private final int width;

    private final int height;

    private final double noiseSharpness;

    private final int noiseResolution;

    private final int smoothingPasses;

    private final String flippedCells;

    private WorldProperties(final Properties properties) {
        seed = properties.getProperty(SEED_PROPERTY, "");
        width = Integer.parseInt(properties.getProperty(WIDTH_PROPERTY, "1"));
        height = Integer.parseInt(properties.getProperty(HEIGHT_PROPERTY, "1"));
        noiseResolution = Integer.parseInt(properties.getProperty(NOISE_RESOLUTION_PROPERTY, "1"));
        noiseSharpness = Double.parseDouble(properties.getProperty(NOISE_SHARPNESS_PROPERTY, "0.5"));
        smoothingPasses = Integer.parseInt(properties.getProperty(SMOOTHING_PASSES_PROPERTY, "1"));
        flippedCells = properties.getProperty(FLIPPED_CELLS_PROPERTY, "");
    }

    public static WorldProperties getFromClasspath() throws IOException {
        final Properties properties = new Properties();
        properties.load(Server.class.getClassLoader().getResourceAsStream(FILENAME));
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
        lines.add(makePropertyLine(WIDTH_PROPERTY, width));
        lines.add(makePropertyLine(HEIGHT_PROPERTY, height));
        lines.add(makePropertyLine(NOISE_RESOLUTION_PROPERTY, noiseResolution));
        lines.add(makePropertyLine(NOISE_SHARPNESS_PROPERTY, noiseSharpness));
        lines.add(makePropertyLine(SMOOTHING_PASSES_PROPERTY, smoothingPasses));
        lines.add(makePropertyLine(FLIPPED_CELLS_PROPERTY, flippedCells));
        return String.join("\n", lines) + "\n";
    }
}
