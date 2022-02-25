package moe.mewore.rabbit.backend.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.PlayerState;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class PlayerUpdateMessage extends BinaryEntity {

    private final int playerId;

    private final PlayerState state;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.UPDATE.getIndex());
        output.writeInt(playerId);
        state.appendToBinaryOutput(output);
    }
}
