package moe.mewore.rabbit.entities.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.entities.world.Forest;

@RequiredArgsConstructor
public class ForestDataMessage extends BinaryEntity {

    private final Forest forest;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.FOREST_DATA.getIndex());
        forest.appendToBinaryOutput(output);
    }
}
