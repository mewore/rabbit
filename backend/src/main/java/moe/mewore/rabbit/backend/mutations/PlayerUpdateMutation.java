package moe.mewore.rabbit.backend.mutations;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.PlayerState;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@Getter
@RequiredArgsConstructor
public class PlayerUpdateMutation extends BinaryEntity {

    private final PlayerState state;

    public static PlayerUpdateMutation decodeFromBinary(final DataInput input) throws IOException {
        return new PlayerUpdateMutation(PlayerState.decodeFromBinary(input));
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MutationType.UPDATE.getIndex());
        state.appendToBinaryOutput(output);
    }
}
