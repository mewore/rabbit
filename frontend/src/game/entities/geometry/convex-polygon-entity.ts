import { Box2, Vector2 } from 'three';
import { BinaryEntity } from '../binary-entity';
import { Segment2D } from './segment-2d';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Vector2Entity } from './vector2-entity';

let result: number;
const tmpVector2 = new Vector2();
let clockwiseCount: number;
let clockwiseResult: number;

export class ConvexPolygonEntity extends BinaryEntity {
    private readonly segments: Segment2D[];
    private readonly aabb: Box2;

    constructor(readonly points: ReadonlyArray<Vector2Entity>) {
        super();
        this.segments = points.map((point, index) => new Segment2D(point, points[(index + 1) % points.length]));
        const pointX = points.map((point) => point.x).sort();
        const pointY = points.map((point) => point.y).sort();
        this.aabb = new Box2(
            new Vector2(pointX[0], pointY[0]),
            new Vector2(pointX[pointX.length - 1], pointY[pointY.length - 1])
        );
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.points.length);
        for (const point of this.points) {
            point.appendToBinaryOutput(writer);
        }
    }

    /**
     * @param point The point to check
     * @return Whether the point is inside the polygon. `false` if the point is exactly on its boundary.
     */
    containsPoint(x: number, y: number): boolean {
        if (!this.aabb.containsPoint(tmpVector2.set(x, y))) {
            return false;
        }
        clockwiseCount = 0;
        for (const segment of this.segments) {
            clockwiseResult = segment.solveForPoint(x, y);
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
            } else if (segment.distanceToPointSquared(x, y) < 0.00001) {
                return false;
            }
        }
        return true;
    }

    distanceToPointSquared(x: number, y: number): number {
        result = Infinity;
        for (const segment of this.segments) {
            result = Math.min(result, segment.distanceToPointSquared(x, y));
        }
        return result;
    }

    static decodeFromBinary(reader: SignedBinaryReader): ConvexPolygonEntity {
        return new ConvexPolygonEntity(reader.readEntityArray(Vector2Entity));
    }
}
