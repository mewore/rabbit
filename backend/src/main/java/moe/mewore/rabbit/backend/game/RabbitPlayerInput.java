package moe.mewore.rabbit.backend.game;

import javax.vecmath.Vector2f;
import java.io.DataInput;
import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@AllArgsConstructor
public class RabbitPlayerInput extends BinaryEntity implements PlayerInput {

    public static final RabbitPlayerInput EMPTY = new RabbitPlayerInput(-1, 0, 0f, -1);

    public static final byte INPUT_UP_BIT = 1;

    public static final byte INPUT_DOWN_BIT = INPUT_UP_BIT << 1;

    public static final byte INPUT_LEFT_BIT = INPUT_DOWN_BIT << 1;

    public static final byte INPUT_RIGHT_BIT = INPUT_LEFT_BIT << 1;

    public static final byte INPUT_JUMP_BIT = INPUT_RIGHT_BIT << 1;

    private static final double EIGHTH = Math.PI / 4;

    private static final double[][] ANGLE_MAP = {makeAngleMapRow(3, 2, 1), makeAngleMapRow(4, 0, 0), makeAngleMapRow(5,
        6, 7)};

    @Getter
    private final int id;

    private final int keys;

    private final float angle;

    /**
     * The ID of the frame this input should have an effect at. Normally, having mutable fields could be a bad idea,
     * especially in the context of parallelism, but generic inheritance doesn't let me use {@link lombok.With}
     * properly, and I don't want to add a separate frame ID in {@link PlayerInputEvent}.
     */
    @Setter
    @Getter
    private long frameId;

    private static double[] makeAngleMapRow(final double xMinusOne, final double xZero, final double xOne) {
        return new double[]{xMinusOne * EIGHTH, xZero * EIGHTH, xOne * EIGHTH};
    }

    public static RabbitPlayerInput decodeFromBinary(final DataInput input) throws IOException {
        final int id = input.readInt();
        final long frameId = input.readLong();
        final int keys = input.readByte();
        final float angle = input.readFloat();
        return new RabbitPlayerInput(id, keys, angle, frameId);
    }

    public static double getTargetAngle(final double angle, final boolean up, final boolean down, final boolean left,
        final boolean right) {
        return ANGLE_MAP[1 + (up ? 1 : 0) - (down ? 1 : 0)][1 + (right ? 1 : 0) - (left ? 1 : 0)] - angle;
    }

    public void applyToTargetHorizontalMotion(final Vector2f targetHorizontalMotion, final float maxSpeed) {
        final boolean up = (keys & RabbitPlayerInput.INPUT_UP_BIT) > 0;
        final boolean down = (keys & RabbitPlayerInput.INPUT_DOWN_BIT) > 0;
        final boolean left = (keys & RabbitPlayerInput.INPUT_LEFT_BIT) > 0;
        final boolean right = (keys & RabbitPlayerInput.INPUT_RIGHT_BIT) > 0;
        if (up || down || left || right) {
            final double actualAngle = getTargetAngle(angle, up, down, left, right);
            targetHorizontalMotion.set((float) (Math.cos(actualAngle) * maxSpeed),
                (float) (Math.sin(actualAngle) * maxSpeed));
        } else {
            targetHorizontalMotion.set(0, 0);
        }
    }

    public boolean isJumping() {
        return (keys & RabbitPlayerInput.INPUT_JUMP_BIT) > 0;
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(id);
        output.writeLong(frameId);
        assert keys < 256 : "The input keys(" + keys + ") should be able to fit into a byte";
        output.writeByte(keys);
        output.writeFloat(angle);
    }
}
