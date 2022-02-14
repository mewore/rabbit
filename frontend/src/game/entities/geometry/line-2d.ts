import { Vector2Entity } from './vector2-entity';

let numerator: number;

export class Line2D {
    protected readonly xCoefficient: number;

    protected readonly yCoefficient: number;

    protected readonly constant: number;

    private readonly distanceCoefficient: number;

    constructor(from: Vector2Entity, to: Vector2Entity) {
        // To derive these lines of code, write down the equations "Ax1 + By1 + C = 0" and "Ax2 + By2 + C = 0" and then
        // subtract the first one from the second one, solve for B and then make A equal "y1 - y2" so that it cancels
        // out with the denominator in B's equation.
        this.xCoefficient = from.y - to.y;
        this.yCoefficient = to.x - from.x;
        this.constant = -(this.xCoefficient * from.x + this.yCoefficient * from.y);

        this.distanceCoefficient =
            1.0 / (this.xCoefficient * this.xCoefficient + this.yCoefficient * this.yCoefficient);
    }

    distanceToPointSquared(x: number, y: number): number {
        // Derived with a bunch of trigonometry. Surprisingly graceful equation.
        numerator = this.xCoefficient * x + this.yCoefficient * y + this.constant;
        return numerator * numerator * this.distanceCoefficient;
    }

    solveForPoint(x: number, y: number): number {
        return this.xCoefficient * x + this.yCoefficient * y + this.constant;
    }
}
