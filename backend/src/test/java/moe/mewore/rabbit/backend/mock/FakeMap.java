package moe.mewore.rabbit.backend.mock;

import java.util.ArrayList;

import moe.mewore.rabbit.world.MazeMap;

public class FakeMap extends MazeMap {

    public FakeMap() {
        super(1, 1, 1.0, new boolean[][]{new boolean[]{true}}, new ArrayList<>(), new int[1][1][0]);
    }
}
