package moe.mewore.rabbit.entities.world;

import java.util.ArrayList;

public class FakeMap extends MazeMap {

    public FakeMap() {
        super(new boolean[][]{new boolean[]{true}}, new ArrayList<>(), new int[1][1][0]);
    }
}
