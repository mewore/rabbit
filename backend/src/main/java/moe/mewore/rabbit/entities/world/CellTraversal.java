package moe.mewore.rabbit.entities.world;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CellTraversal {

    public final int minDistance;

    public final int fromRow;

    public final int fromColumn;
}
