package moe.mewore.rabbit.entities.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.entities.Player;

@RequiredArgsConstructor
public class PlayerJoinMessage extends BinaryEntity {

    private final Player player;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.JOIN.getIndex());
        player.appendToBinaryOutput(output);
    }
}
