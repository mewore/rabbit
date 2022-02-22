package moe.mewore.rabbit.noise.composite;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.noise.Noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeNoiseTest {

    @Test
    void testXnor() {
        final Noise first = mock(Noise.class);
        when(first.get(.5, .5)).thenReturn(0.5);

        final Noise second = mock(Noise.class);
        when(second.get(.5, .5)).thenReturn(0.5);

        assertEquals(1.0, new CompositeNoise(first, second, CompositeNoise.XNOR_BLENDING).get(.5, .5));
    }
}
