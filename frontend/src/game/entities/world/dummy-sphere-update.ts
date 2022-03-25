import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Vector3Entity } from '../geometry/vector3-entity';

export class DummySphereUpdate extends BinaryEntity {
    constructor(readonly position: Vector3Entity, readonly motion: Vector3Entity) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.position.appendToBinaryOutput(writer);
        this.motion.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): DummySphereUpdate {
        return new DummySphereUpdate(Vector3Entity.decodeFromBinary(reader), Vector3Entity.decodeFromBinary(reader));
    }
}
