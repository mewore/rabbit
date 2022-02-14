package moe.mewore.rabbit.entities.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.entities.world.MazeMap;

@RequiredArgsConstructor
public class MapDataMessage extends BinaryEntity {

    private final MazeMap map;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.MAP_DATA.getIndex());
        map.appendToBinaryOutput(output);
    }
}
