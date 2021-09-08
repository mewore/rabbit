package moe.mewore.rabbit.entities.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlantTest {

    @Test
    void testEncode() {
        assertEquals(9, new Plant(.1, .2, .3).encodeToBinary().length);
    }
}
