package moe.mewore.rabbit.geometry;

import java.io.DataInput;
import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@Getter
@Setter
@AllArgsConstructor
public class Vector3 extends BinaryEntity implements GeometricVector {

    private double x;

    private double y;

    private double z;

    public Vector3() {
        this(0, 0, 0);
    }

    public static Vector3 decodeFromBinary(final DataInput input) throws IOException {
        return new Vector3(input.readDouble(), input.readDouble(), input.readDouble());
    }

    public int store(final double[] source, int position) {
        source[position] = x;
        source[++position] = y;
        source[++position] = z;
        return ++position;
    }

    public int load(final double[] source, int position) {
        x = source[position];
        y = source[++position];
        z = source[++position];
        return ++position;
    }

    public void set(final double newX, final double newY, final double newZ) {
        x = newX;
        y = newY;
        z = newZ;
    }

    public void add(final double dx, final double dy, final double dz) {
        x += dx;
        y += dy;
        z += dz;
    }

    public void addScaledVector(final Vector3 otherVector, final double scalar) {
        add(otherVector.getX() * scalar, otherVector.getY() * scalar, otherVector.getZ() * scalar);
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeDouble(x);
        output.writeDouble(y);
        output.writeDouble(z);
    }
}
