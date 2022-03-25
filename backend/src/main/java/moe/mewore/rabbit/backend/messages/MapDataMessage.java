package moe.mewore.rabbit.backend.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.physics.PhysicsDummyBox;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.world.MazeMap;

@RequiredArgsConstructor
public class MapDataMessage extends BinaryEntity {

    private final MazeMap map;

    private final PhysicsDummyBox[] boxes;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.MAP_DATA.getIndex());
        map.appendToBinaryOutput(output);
        output.writeArray(boxes);
    }
}
