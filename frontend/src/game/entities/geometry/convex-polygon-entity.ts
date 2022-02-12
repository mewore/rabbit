import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Vector2Entity } from './vector2-entity';

export class ConvexPolygonEntity extends BinaryEntity {
    constructor(readonly points: ReadonlyArray<Vector2Entity>) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.points.length);
        for (const point of this.points) {
            point.appendToBinaryOutput(writer);
        }
    }

    static decodeFromBinary(reader: SignedBinaryReader): ConvexPolygonEntity {
        return new ConvexPolygonEntity(reader.readEntityArray(Vector2Entity));
    }
}
