import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Vector2 } from 'three';

export class Vector2Entity extends BinaryEntity {
    constructor(readonly x: number, readonly y: number) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeDouble(this.x);
        writer.writeDouble(this.y);
    }

    distanceToSquared(x: number, y: number): number {
        const dx = this.x - x;
        const dy = this.y - y;
        return dx * dx + dy * dy;
    }

    static decodeFromBinary(reader: SignedBinaryReader): Vector2Entity {
        return new Vector2Entity(reader.readDouble(), reader.readDouble());
    }

    static fromVector2(vector2: Vector2): Vector2Entity {
        return new Vector2Entity(vector2.x, vector2.y);
    }
}
