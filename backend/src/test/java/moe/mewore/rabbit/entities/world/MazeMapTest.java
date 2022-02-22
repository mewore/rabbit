package moe.mewore.rabbit.entities.world;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.noise.Noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MazeMapTest {

    @Test
    void testGenerate() {
        final Random random = new Random(11L);
        final Noise opennessNoise = mock(Noise.class);
        when(opennessNoise.get(anyDouble(), anyDouble())).thenReturn(.5);

        // ~12KB
        assertEquals(12712, MazeMap.createSeamless(30, 30, random, 3, opennessNoise).encodeToBinary().length);
    }

    @Test
    void testEncode() {
        final MazeMap map = new MazeMap(new boolean[3][3], new ArrayList<>(), new int[3][3][0]);
        assertEquals(21, map.encodeToBinary().length);
    }
}
