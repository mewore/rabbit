package moe.mewore.rabbit.backend.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class PlayerJoinMessage extends BinaryEntity {

    private final Player player;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.JOIN.getIndex());
        player.appendToBinaryOutput(output);
    }
}
