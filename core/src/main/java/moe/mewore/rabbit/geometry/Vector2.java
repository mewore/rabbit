package moe.mewore.rabbit.geometry;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@Getter
@RequiredArgsConstructor
public class Vector2 extends BinaryEntity {

    public static final Vector2 ZERO = new Vector2(0, 0);

    private final double x;

    private final double y;

    public double distanceToSquared(final Vector2 other) {
        final double dx = other.getX() - getX();
        final double dy = other.getY() - getY();
        return dx * dx + dy * dy;
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
