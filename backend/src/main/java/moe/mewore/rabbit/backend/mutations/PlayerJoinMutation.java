package moe.mewore.rabbit.backend.mutations;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@Getter
@RequiredArgsConstructor
public class PlayerJoinMutation extends BinaryEntity {

    private final boolean isReisen;

    public static PlayerJoinMutation decodeFromBinary(final DataInput input) throws IOException {
        return new PlayerJoinMutation(input.readBoolean());
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MutationType.PLAYER_JOIN.getIndex());
        output.writeBoolean(isReisen);
    }
}
