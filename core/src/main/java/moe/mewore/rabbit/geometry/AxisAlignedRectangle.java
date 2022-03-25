package moe.mewore.rabbit.geometry;

import java.util.List;

public class AxisAlignedRectangle {

    private final float minX;

    private final float maxX;

    private final float minY;

    private final float maxY;

    public AxisAlignedRectangle(final List<Vector2> points) {
        minX = points.stream().map(Vector2::getX).min(Float::compareTo).orElse(0f);
        maxX = points.stream().map(Vector2::getX).max(Float::compareTo).orElse(0f);
        minY = points.stream().map(Vector2::getY).min(Float::compareTo).orElse(0f);
        maxY = points.stream().map(Vector2::getY).max(Float::compareTo).orElse(0f);
    }

    public boolean containsPoint(final Vector2 point) {
        return point.getX() > minX && point.getX() < maxX && point.getY() > minY && point.getY() < maxY;
    }
}
