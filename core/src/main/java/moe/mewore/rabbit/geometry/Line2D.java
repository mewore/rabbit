package moe.mewore.rabbit.geometry;

public class Line2D {

    protected final double xCoefficient;

    protected final double yCoefficient;

    protected final double constant;

    private final double distanceCoefficient;

    Line2D(final Vector2 from, final Vector2 to) {
        // To derive these lines of code, write down the equations "Ax1 + By1 + C = 0" and "Ax2 + By2 + C = 0" and then
        // subtract the first one from the second one, solve for B and then make A equal "y1 - y2" so that it cancels
        // out with the denominator in B's equation.
        xCoefficient = from.getY() - to.getY();
        yCoefficient = to.getX() - from.getX();
        constant = -(xCoefficient * from.getX() + yCoefficient * from.getY());

        distanceCoefficient = 1.0 / (xCoefficient * xCoefficient + yCoefficient * yCoefficient);
    }

    public double distanceToPointSquared(final Vector2 point) {
        // Derived with a bunch of trigonometry. Surprisingly graceful equation.
        final double numerator = xCoefficient * point.getX() + yCoefficient * point.getY() + constant;
        return numerator * numerator * distanceCoefficient;
    }

    public double solveForPoint(final Vector2 point) {
        return xCoefficient * point.getX() + yCoefficient * point.getY() + constant;
    }
}
