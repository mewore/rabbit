package moe.mewore.rabbit.geometry;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;

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

    public static Vector2 decodeFromBinary(final DataInput input) throws IOException {
        return new Vector2(input.readDouble(), input.readDouble());
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeDouble(x);
        output.writeDouble(y);
    }
}
