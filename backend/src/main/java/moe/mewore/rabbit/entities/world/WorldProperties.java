package moe.mewore.rabbit.entities.world;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

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

    private final String seed;

    private final int width;

    private final int height;

    private final double noiseSharpness;

    private final int noiseResolution;

    private final int smoothingPasses;

    private WorldProperties(final Properties properties) {
        seed = properties.getProperty(SEED_PROPERTY);
        width = Integer.parseInt(properties.getProperty(WIDTH_PROPERTY));
        height = Integer.parseInt(properties.getProperty(HEIGHT_PROPERTY));
        noiseResolution = Integer.parseInt(properties.getProperty(NOISE_RESOLUTION_PROPERTY));
        noiseSharpness = Double.parseDouble(properties.getProperty(NOISE_SHARPNESS_PROPERTY));
        smoothingPasses = Integer.parseInt(properties.getProperty(SMOOTHING_PASSES_PROPERTY));
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

    private static String makePropertyLine(final String key, final Object value) {
        return key + "=" + value;
    }

    public void save(final File target) throws IOException {
        if (target.createNewFile()) {
            System.out.println(target.getAbsolutePath() + " has been created.");
        }

        try (final FileWriter writer = new FileWriter(target, StandardCharsets.ISO_8859_1)) {
            writer.write(String.join("\n",
                new String[]{makePropertyLine(SEED_PROPERTY, seed), makePropertyLine(WIDTH_PROPERTY,
                    width), makePropertyLine(HEIGHT_PROPERTY, height), makePropertyLine(NOISE_RESOLUTION_PROPERTY,
                    noiseResolution), makePropertyLine(NOISE_SHARPNESS_PROPERTY, noiseSharpness), makePropertyLine(
                    SMOOTHING_PASSES_PROPERTY, smoothingPasses),}) + "\n");
        }
    }
}
