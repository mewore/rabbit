package moe.mewore.rabbit.backend.mutations;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class PlayerInputMutation extends BinaryEntity {

    public static final int INPUT_UP_BIT = 1;

    public static final int INPUT_DOWN_BIT = INPUT_UP_BIT << 1;

    public static final int INPUT_LEFT_BIT = INPUT_DOWN_BIT << 1;

    public static final int INPUT_RIGHT_BIT = INPUT_LEFT_BIT << 1;

    private static final double EIGHTH = Math.PI / 4;

    private static final double[][] ANGLE_MAP = {makeAngleMapRow(3, 2, 1), makeAngleMapRow(4, 0, 0), makeAngleMapRow(5,
        6, 7)};

    @Getter
    private final int id;

    @Getter
    private final int frameId;

    @Getter
    private final float angle;

    @Getter
    private final byte keys;

    @Getter
    @Setter
    private int timestamp;

    private static double[] makeAngleMapRow(final double xMinusOne, final double xZero, final double xOne) {
        return new double[]{xMinusOne * EIGHTH, xZero * EIGHTH, xOne * EIGHTH};
    }

    public static PlayerInputMutation decodeFromBinary(final DataInput input) throws IOException {
        return new PlayerInputMutation(input.readInt(), input.readInt(), input.readFloat(), input.readByte());
    }

    public static double getTargetAngle(final double angle, final boolean up, final boolean down, final boolean left,
        final boolean right) {
        return ANGLE_MAP[1 + (up ? 1 : 0) - (down ? 1 : 0)][1 + (right ? 1 : 0) - (left ? 1 : 0)] - angle;
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MutationType.PLAYER_INPUT.getIndex());
        output.writeInt(id);
        output.writeInt(frameId);
        output.writeFloat(angle);
        output.writeByte(keys);
    }
}
