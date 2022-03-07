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
public class Vector2 extends BinaryEntity implements GeometricVector {

    private double x;

    private double y;

    public Vector2() {
        this(0, 0);
    }

    public double distanceToSquared(final Vector2 other) {
        final double dx = other.getX() - getX();
        final double dy = other.getY() - getY();
        return dx * dx + dy * dy;
    }

    public int store(final double[] source, int position) {
        source[position] = x;
        source[++position] = y;
        return ++position;
    }

    public int load(final double[] source, int position) {
        x = source[position];
        y = source[++position];
        return ++position;
    }

    public void set(final double newX, final double newY) {
        x = newX;
        y = newY;
    }

    public Vector2 plus(final double deltaX, final double deltaY) {
        return new Vector2(x + deltaX, y + deltaY);
    }

    public static Vector2 decodeFromBinary(final DataInput input) throws IOException {
        return new Vector2(input.readDouble(), input.readDouble());
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeDouble(x);
        output.writeDouble(y);
    }
}
