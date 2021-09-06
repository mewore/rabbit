package moe.mewore.rabbit.entities.events;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;

@RequiredArgsConstructor
public class PlayerSetUpEvent extends BinaryEntity {

    private final int playerId;

    private final boolean isReisen;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(EventType.SET_UP.getIndex());
        output.writeInt(playerId);
        output.writeBoolean(isReisen);
    }
}
