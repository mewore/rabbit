package moe.mewore.rabbit.backend.physics;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.geometry.ConvexPolygon;
import moe.mewore.rabbit.geometry.Vector2;
import moe.mewore.rabbit.world.MazeMap;
import moe.mewore.rabbit.world.MazeWall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ForestWallsTest {

    @Test
    void testGenerate() {
        final MazeMap map = mock(MazeMap.class);
        final List<MazeWall> walls = new ArrayList<>();
        walls.add(new MazeWall(0, 0, 1, 1,
            new ConvexPolygon(List.of(new Vector2(0, 0), new Vector2(0, 1), new Vector2(1, 1), new Vector2(1, 0)))));
        walls.add(new MazeWall(0, 0, 2, 2,
            new ConvexPolygon(List.of(new Vector2(0, 1), new Vector2(2, 2), new Vector2(1, 0)))));
        when(map.getWalls()).thenReturn(walls);

        final ForestWalls forestWalls = ForestWalls.generate(map);
        assertEquals(2, forestWalls.getBodies().length);
    }
}
