package moe.mewore.rabbit.backend.simulation;

import lombok.Getter;
import lombok.Setter;

@Setter
public class WorldSnapshot {

    @Getter
    private final int[] intData;

    @Getter
    private final float[] floatData;

    public WorldSnapshot(final int intCount, final int floatCount) {
        intData = new int[intCount];
        floatData = new float[floatCount];
    }

    public void copy(final WorldSnapshot previous) {
        System.arraycopy(previous.intData, 0, intData, 0, intData.length);
        System.arraycopy(previous.floatData, 0, floatData, 0, floatData.length);
    }
}
