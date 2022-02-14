import { Line2D } from './line-2d';
import { Vector2Entity } from './vector2-entity';

export class Segment2D extends Line2D {
    private readonly segmentMinConstant: number;

    private readonly segmentMaxConstant: number;

    constructor(private readonly from: Vector2Entity, private readonly to: Vector2Entity) {
        super(from, to);

        const fromPerpendicularConstant = this.xCoefficient * from.y - this.yCoefficient * from.x;
        const toPerpendicularConstant = this.xCoefficient * to.y - this.yCoefficient * to.x;
        this.segmentMinConstant = Math.min(fromPerpendicularConstant, toPerpendicularConstant);
        this.segmentMaxConstant = Math.max(fromPerpendicularConstant, toPerpendicularConstant);
    }

    distanceToPointSquared(x: number, y: number): number {
        if (!this.pointFallsOnSegment(x, y)) {
            return Math.min(this.from.distanceToSquared(x, y), this.to.distanceToSquared(x, y));
        }
        return super.distanceToPointSquared(x, y);
    }

    pointFallsOnSegment(x: number, y: number): boolean {
        const pointConstant = this.xCoefficient * y - this.yCoefficient * x;
        return pointConstant > this.segmentMinConstant && pointConstant < this.segmentMaxConstant;
    }
}
