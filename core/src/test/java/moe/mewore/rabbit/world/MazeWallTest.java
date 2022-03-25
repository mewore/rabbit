package moe.mewore.rabbit.world;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.geometry.ConvexPolygon;
import moe.mewore.rabbit.geometry.Vector2;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MazeWallTest {

    @Test
    void testEncode() {
        final MazeWall wall = new MazeWall(0, 1, 2, 3, new ConvexPolygon(
            Arrays.asList(new Vector2(0, 0), new Vector2(5, 0), new Vector2(7, 3), new Vector2(3, 6),
                new Vector2(-2, 4))));
        assertEquals(60, wall.encodeToBinary().length);
    }
}
