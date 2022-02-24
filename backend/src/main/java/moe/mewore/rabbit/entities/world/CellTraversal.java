package moe.mewore.rabbit.entities.world;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CellTraversal {

    private final double minDistance;

    private final int dx;

    private final int dy;
}
