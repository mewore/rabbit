import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

function roundNumber(num: number) {
    return Math.round(num * 100) / 100;
}

type AnyVector3 = { x: number; y: number; z: number };

interface Vector3WithSet {
    setValue(x: number, y: number, z: number): void;
}

function isVectorWithSet(vector: AnyVector3 | Vector3WithSet): vector is Vector3WithSet {
    return !!(vector as unknown as Vector3WithSet).setValue;
}

export class Vector3Entity extends BinaryEntity {
    constructor(readonly x: number, readonly y: number, readonly z: number) {
        super();
    }

    paste(target: AnyVector3 | Vector3WithSet) {
        if (isVectorWithSet(target)) {
            target.setValue(this.x, this.y, this.z);
        } else {
            target.x = this.x;
            target.y = this.y;
            target.z = this.z;
        }
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);
    }

    static decodeFromBinary(reader: SignedBinaryReader): Vector3Entity {
        return new Vector3Entity(reader.readFloat(), reader.readFloat(), reader.readFloat());
    }

    static fromVector3(vector3: AnyVector3): Vector3Entity {
        return new Vector3Entity(vector3.x, vector3.y, vector3.z);
    }

    toString(): string {
        return `(${roundNumber(this.x)}, ${roundNumber(this.y)}, ${roundNumber(this.z)})`;
    }
}
