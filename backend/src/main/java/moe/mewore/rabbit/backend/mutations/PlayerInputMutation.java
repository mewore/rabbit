package moe.mewore.rabbit.backend.mutations;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class PlayerInputMutation extends BinaryEntity {

    @Getter
    private final PlayerInput input;

    public static PlayerInputMutation decodeFromBinary(final DataInput input) throws IOException {
        return new PlayerInputMutation(PlayerInput.decodeFromBinary(input));
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MutationType.PLAYER_INPUT.getIndex());
        input.appendToBinaryOutput(output);
    }
}
