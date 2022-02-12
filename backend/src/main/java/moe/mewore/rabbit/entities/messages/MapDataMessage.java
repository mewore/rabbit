package moe.mewore.rabbit.entities.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.entities.world.Forest;
import moe.mewore.rabbit.generation.MazeMap;

@RequiredArgsConstructor
public class MapDataMessage extends BinaryEntity {

    private final MazeMap map;

    private final Forest forest;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.MAP_DATA.getIndex());
        map.appendToBinaryOutput(output);
        forest.appendToBinaryOutput(output);
    }
}
