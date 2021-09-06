import { BinaryEntity } from './binary-entity';
import { SignedBinaryReader } from './data/signed-binary-reader';
import { SignedBinaryWriter } from './data/signed-binary-writer';
import { Vector3 } from 'three';

export class Vector3Entity extends BinaryEntity {
    constructor(readonly x: number, readonly y: number, readonly z: number) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeDouble(this.x);
        writer.writeDouble(this.y);
        writer.writeDouble(this.z);
    }

    static decodeFromBinary(reader: SignedBinaryReader): Vector3Entity {
        return new Vector3Entity(reader.readDouble(), reader.readDouble(), reader.readDouble());
    }

    static fromVector3(vector3: Vector3): Vector3Entity {
        return new Vector3Entity(vector3.x, vector3.y, vector3.z);
    }
}