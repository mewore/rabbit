package moe.mewore.rabbit.entities.world;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MazeMapTest {

    @Test
    void testGenerate() {
        final Random random = new Random(11L);
        // ~15KB
        assertEquals(15628, MazeMap.createSeamless(30, 30, random, 3).encodeToBinary().length);
    }

    @Test
    void testEncode() {
        final MazeMap map = new MazeMap(new boolean[3][3], new ArrayList<>(), new int[3][3][0]);
        assertEquals(21, map.encodeToBinary().length);
    }
}
