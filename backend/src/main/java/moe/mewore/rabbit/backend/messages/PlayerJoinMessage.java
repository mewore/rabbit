package moe.mewore.rabbit.backend.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class PlayerJoinMessage extends BinaryEntity {

    private final int playerId;

    private final String username;

    private final boolean isReisen;

    private final boolean isSelf;

    public PlayerJoinMessage(final Player player, final boolean isSelf) {
        this(player.getId(), player.getUsername(), player.isReisen(), isSelf);
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.JOIN.getIndex());
        output.writeInt(playerId);
        output.writeAsciiWithLength(username);
        output.writeBoolean(isReisen);
        output.writeBoolean(isSelf);
    }
}
