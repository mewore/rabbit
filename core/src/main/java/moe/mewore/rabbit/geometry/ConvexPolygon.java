package moe.mewore.rabbit.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

public class ConvexPolygon extends BinaryEntity {

    @Getter
    private final List<Vector2> points;

    private final List<Segment2D> segments;

    private final AxisAlignedRectangle aabb;

    public ConvexPolygon(final List<Vector2> points) {
        this.points = Collections.unmodifiableList(points);
        segments = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            segments.add(new Segment2D(points.get(i), points.get((i + 1) % points.size())));
        }
        aabb = new AxisAlignedRectangle(points);
    }

    public ConvexPolygon withOffset(final Vector2 offset) {
        final List<Vector2> newPoints = new ArrayList<>(points.size());
        for (final Vector2 point : points) {
            newPoints.add(new Vector2(point.getX() + offset.getX(), point.getY() + offset.getY()));
        }
        return new ConvexPolygon(newPoints);
    }

    /**
     * @param point The point to check
     * @return Whether the point is inside the polygon. `false` if the point is exactly on its boundary.
     */
    public boolean containsPoint(final Vector2 point) {
        if (!aabb.containsPoint(point)) {
            return false;
        }
        int clockwiseCount = 0;
        double clockwiseResult;
        for (final Line2D segment : segments) {
            clockwiseResult = segment.solveForPoint(point);
            if (clockwiseResult > 0.00001) {
                if (clockwiseCount < 0) {
                    return false;
                }
                clockwiseCount = 1;
            } else if (clockwiseResult < -0.00001) {
                if (clockwiseCount > 0) {
                    return false;
                }
                clockwiseCount--;
            } else if (segment.distanceToPointSquared(point) < 0.00001) {
                return false;
            }
        }
        return true;
    }

    public double distanceToPointSquared(final Vector2 point) {
        double result = -1.0;
        double currentDistance;
        for (final Line2D segment : segments) {
            currentDistance = segment.distanceToPointSquared(point);
            result = result < 0 ? currentDistance : Math.min(result, currentDistance);
        }
        return result;
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(points.size());
        for (final Vector2 point : points) {
            point.appendToBinaryOutput(output);
        }
    }
}
