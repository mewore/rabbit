package moe.mewore.rabbit.entities.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.entities.PlayerState;

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
