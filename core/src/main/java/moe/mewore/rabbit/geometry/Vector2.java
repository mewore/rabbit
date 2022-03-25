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
public class Vector2 extends BinaryEntity {

    private float x;

    private float y;

    public Vector2() {
        this(0, 0);
    }

    public double distanceToSquared(final Vector2 other) {
        final double dx = other.getX() - getX();
        final double dy = other.getY() - getY();
        return dx * dx + dy * dy;
    }

    public static Vector2 decodeFromBinary(final DataInput input) throws IOException {
        return new Vector2(input.readFloat(), input.readFloat());
    }

    public void set(final float newX, final float newY) {
        x = newX;
        y = newY;
    }

    public Vector2 plus(final float deltaX, final float deltaY) {
        return new Vector2(x + deltaX, y + deltaY);
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeFloat(x);
        output.writeFloat(y);
    }
}
