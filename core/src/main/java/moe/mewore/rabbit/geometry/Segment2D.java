package moe.mewore.rabbit.geometry;

public class Segment2D extends Line2D {

    private final Vector2 from;

    private final Vector2 to;

    private final double segmentMinConstant;

    private final double segmentMaxConstant;

    Segment2D(final Vector2 from, final Vector2 to) {
        super(from, to);
        this.from = from;
        this.to = to;

        final double fromPerpendicularConstant = xCoefficient * from.getY() - yCoefficient * from.getX();
        final double toPerpendicularConstant = xCoefficient * to.getY() - yCoefficient * to.getX();
        segmentMinConstant = Math.min(fromPerpendicularConstant, toPerpendicularConstant);
        segmentMaxConstant = Math.max(fromPerpendicularConstant, toPerpendicularConstant);
    }

    @Override
    public double distanceToPointSquared(final Vector2 point) {
        if (!pointFallsOnSegment(point)) {
            return Math.min(from.distanceToSquared(point), to.distanceToSquared(point));
        }
        return super.distanceToPointSquared(point);
    }

    private boolean pointFallsOnSegment(final Vector2 point) {
        final double pointConstant = xCoefficient * point.getY() - yCoefficient * point.getX();
        return pointConstant > segmentMinConstant && pointConstant < segmentMaxConstant;
    }
}
