package moe.mewore.rabbit.entities.world;

import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;

public class Plant extends BinaryEntity {

    private static final byte MAX_HEIGHT = 127;

    private final float x;

    private final float y;

    private final byte height;

    Plant(final double x, final double y, final double height) {
        this.x = (float) x;
        this.y = (float) y;
        this.height = (byte) (height * MAX_HEIGHT);
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeFloat(x);
        output.writeFloat(y);
        output.writeByte(height);
    }
}
