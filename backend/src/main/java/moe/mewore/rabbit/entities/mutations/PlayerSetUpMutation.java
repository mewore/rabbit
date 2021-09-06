package moe.mewore.rabbit.entities.mutations;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;

@Getter
@RequiredArgsConstructor
public class PlayerSetUpMutation extends BinaryEntity {

    private final boolean isReisen;

    public static PlayerSetUpMutation decodeFromBinary(final DataInput input) throws IOException {
        return new PlayerSetUpMutation(input.readBoolean());
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MutationType.SET_UP.getIndex());
        output.writeBoolean(isReisen);
    }
}
