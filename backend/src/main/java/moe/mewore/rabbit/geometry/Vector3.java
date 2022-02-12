package moe.mewore.rabbit.geometry;

import java.io.DataInput;
import java.io.IOException;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;

@RequiredArgsConstructor
public class Vector3 extends BinaryEntity {

    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    private final double x;

    private final double y;

    private final double z;

    public static Vector3 decodeFromBinary(final DataInput input) throws IOException {
        return new Vector3(input.readDouble(), input.readDouble(), input.readDouble());
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeDouble(x);
        output.writeDouble(y);
        output.writeDouble(z);
    }
}
