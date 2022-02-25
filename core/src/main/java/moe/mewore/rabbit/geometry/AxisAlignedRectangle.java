package moe.mewore.rabbit.geometry;

import java.util.List;

public class AxisAlignedRectangle {

    private final double minX;

    private final double maxX;

    private final double minY;

    private final double maxY;

    public AxisAlignedRectangle(final List<Vector2> points) {
        minX = points.stream().map(Vector2::getX).min(Double::compareTo).orElse(0.0);
        maxX = points.stream().map(Vector2::getX).max(Double::compareTo).orElse(0.0);
        minY = points.stream().map(Vector2::getY).min(Double::compareTo).orElse(0.0);
        maxY = points.stream().map(Vector2::getY).max(Double::compareTo).orElse(0.0);
    }

    public boolean containsPoint(final Vector2 point) {
        return point.getX() > minX && point.getX() < maxX && point.getY() > minY && point.getY() < maxY;
    }
}
