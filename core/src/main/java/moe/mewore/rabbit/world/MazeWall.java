package moe.mewore.rabbit.world;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.geometry.ConvexPolygon;

@RequiredArgsConstructor
public class MazeWall extends BinaryEntity {

    private final int topRow;

    private final int leftColumn;

    private final int bottomRow;

    private final int rightColumn;

    @Getter
    private final ConvexPolygon polygon;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(topRow);
        output.writeInt(leftColumn);
        output.writeInt(bottomRow);
        output.writeInt(rightColumn);
        polygon.appendToBinaryOutput(output);
    }
}
