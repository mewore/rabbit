package moe.mewore.rabbit.entities.world;

import java.util.Random;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.generation.MazeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ForestTest {

    @Test
    void testGenerate() {
        final MazeMap map = mock(MazeMap.class);
        when(map.get(anyDouble(), anyDouble())).thenReturn(0.5);
        final Forest forest = Forest.generate(map, new Random(25L));
        assertEquals(36868, forest.encodeToBinary().length);
    }

    @Test
    void testEncode() {
        assertEquals(13, new Forest(new Plant[]{new Plant(.1, .2, .3)}).encodeToBinary().length);
    }
}
