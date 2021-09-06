package moe.mewore.rabbit.entities.events;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.entities.Player;

@RequiredArgsConstructor
public class PlayerDisconnectEvent extends BinaryEntity {

    private final Player player;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(EventType.DISCONNECT.getIndex());
        output.writeInt(player.getId());
    }
}
