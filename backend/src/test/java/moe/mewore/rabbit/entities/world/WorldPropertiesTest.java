package moe.mewore.rabbit.entities.world;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    }

    @Test
    void testGetSeedAsLong() {
        assertEquals(11L, new WorldProperties("11", 1, 1, 1, 1, 1).getSeedAsLong());
    }
}
