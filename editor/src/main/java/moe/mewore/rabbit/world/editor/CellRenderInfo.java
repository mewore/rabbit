package moe.mewore.rabbit.world.editor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Information about the position and scale of the rectangle corresponding to a map cell.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class CellRenderInfo {

    private final int x;

    private final int y;

    private final int width;

    private final int height;

    CellRenderInfo(final int cell, final int mapWidth, final int mapHeight, final int imageWidth,
        final int imageHeight) {
        this(((cell % mapWidth) * imageWidth) / mapWidth, ((cell / mapWidth) * imageHeight) / mapHeight,
            imageWidth / mapWidth, imageHeight / mapHeight);
    }
}
