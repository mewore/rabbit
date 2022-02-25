package moe.mewore.rabbit.world.editor;

import java.util.ArrayList;

import moe.mewore.rabbit.world.MazeMap;

public class FakeMap extends MazeMap {

    public FakeMap() {
        super(1, 1, new boolean[][]{new boolean[]{true}}, new ArrayList<>(), new int[1][1][0]);
    }
}
