package moe.mewore.rabbit.entities.world;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForestTest {

    @Test
    void testGenerate() {
        final Forest forest = Forest.generate(new Random(25L));
        assertEquals(36868, forest.encodeToBinary().length);
    }

    @Test
    void testEncode() {
        assertEquals(13, new Forest(new Plant[]{new Plant(.1, .2, .3)}).encodeToBinary().length);
    }
}
