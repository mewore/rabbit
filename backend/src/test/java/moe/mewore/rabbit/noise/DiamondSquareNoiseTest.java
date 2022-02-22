package moe.mewore.rabbit.noise;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiamondSquareNoiseTest {

    private Random random;

    @BeforeEach
    void setUp() {
        random = new Random(25L);
    }

    @Test
    void testCreateSeamless() {
        final Noise noise = DiamondSquareNoise.createSeamless(3, random, null, .5);
        assertEquals(0.22594186158633173, noise.get(0.1, 0.4));
    }

    @Test
    void testCreateSeamless_zeroAtCenter() {
        final Noise noise = DiamondSquareNoise.createSeamless(3, random, 0.0, .5);
        assertEquals(0.0, noise.get(.5, .5));
    }

    @Test
    void testCreateSeamless_resolutionTooSmall() {
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> DiamondSquareNoise.createSeamless(-1, random, 0.0, .5));
        assertEquals("A resolution of -1 is not allowed; the resolution should be non-negative",
            exception.getMessage());
    }

    @Test
    void testCreateSeamless_resolutionTooLarge() {
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> DiamondSquareNoise.createSeamless(42, random, 0.0, .5));
        assertEquals("A resolution of 42 is too large (12 is the largest allowed one)", exception.getMessage());
    }

    @Test
    void testGet_wrapped_negativeX_positiveY() {
        final Noise noise = DiamondSquareNoise.createSeamless(3, random, null, .5);
        assertEquals(0.7163247059300404, noise.get(-2649.0, 1.2));
    }

    @Test
    void testGet_wrapped_positiveX_negativeY() {
        final Noise noise = DiamondSquareNoise.createSeamless(3, random, null, .5);
        assertEquals(0.44829919981463595, noise.get(2649.0, -1.2));
    }
}
