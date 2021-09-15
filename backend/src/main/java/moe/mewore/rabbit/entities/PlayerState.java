package moe.mewore.rabbit.entities;

import java.io.DataInput;
import java.io.IOException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PlayerState extends BinaryEntity {

    private final Vector3 position;

    private final Vector3 motion;

    private final Vector2 targetHorizontalMotion;

    PlayerState() {
        this(Vector3.ZERO, Vector3.ZERO, Vector2.ZERO);
    }

    public static PlayerState decodeFromBinary(final DataInput input) throws IOException {
        return new PlayerState(Vector3.decodeFromBinary(input), Vector3.decodeFromBinary(input),
            Vector2.decodeFromBinary(input));
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        position.appendToBinaryOutput(output);
        motion.appendToBinaryOutput(output);
        targetHorizontalMotion.appendToBinaryOutput(output);
    }
}
