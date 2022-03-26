package moe.mewore.rabbit.backend.simulation;

import lombok.Getter;
import lombok.Setter;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@Setter
public class WorldSnapshot extends BinaryEntity {

    @Getter
    private final int[] intData;

    @Getter
    private final float[] floatData;

    public WorldSnapshot(final int intCount, final int floatCount) {
        intData = new int[intCount];
        floatData = new float[floatCount];
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(intData.length);
        for (final int intDatum : intData) {
            output.writeInt(intDatum);
        }
        for (final float floatDatum : floatData) {
            output.writeFloat(floatDatum);
        }
    }
}
