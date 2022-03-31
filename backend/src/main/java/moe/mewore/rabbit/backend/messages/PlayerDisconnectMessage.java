package moe.mewore.rabbit.backend.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.game.RabbitPlayer;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class PlayerDisconnectMessage extends BinaryEntity {

    private final RabbitPlayer player;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.DISCONNECT.getIndex());
        output.writeInt(player.getIndex());
    }
}
