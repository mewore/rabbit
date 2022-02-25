package moe.mewore.rabbit.world;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorldPropertiesTest {

    @Test
    void testGetFromClasspath() throws IOException {
        final WorldProperties worldProperties = WorldProperties.getFromClasspath();
        assertEquals("1", worldProperties.getSeed());
        assertEquals(2, worldProperties.getWidth());
        assertEquals(3, worldProperties.getHeight());
        assertEquals(4, worldProperties.getNoiseResolution());
        assertEquals(0.5, worldProperties.getNoiseSharpness());
        assertEquals(6, worldProperties.getSmoothingPasses());
        assertArrayEquals(new Integer[]{1, 2}, worldProperties.getFlippedCellSet().stream().sorted().toArray());
    }

    @Test
    void testGetSeedAsLong() {
        assertEquals(11L, new WorldProperties("11", 1, 1, 1, 1, 1, "").getSeedAsLong());
    }

    @Test
    void testAsString() throws IOException, URISyntaxException {
        final WorldProperties worldProperties = WorldProperties.getFromClasspath();
        final URL propertiesFileUrl = getClass().getClassLoader().getResource(WorldProperties.FILENAME);
        assertNotNull(propertiesFileUrl);
        final String expected = Files.readString(new File(propertiesFileUrl.toURI()).toPath(),
            StandardCharsets.ISO_8859_1);
        assertEquals(expected, worldProperties.asString());
    }
}
