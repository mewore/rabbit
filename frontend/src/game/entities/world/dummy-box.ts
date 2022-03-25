import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Vector3Entity } from '../geometry/vector3-entity';

export class DummyBox extends BinaryEntity {
    constructor(
        readonly width: number,
        readonly height: number,
        readonly position: Vector3Entity,
        readonly rotationY: number
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeFloat(this.width);
        writer.writeFloat(this.height);
        this.position.appendToBinaryOutput(writer);
        writer.writeFloat(this.rotationY);
    }
    static decodeFromBinary(reader: SignedBinaryReader): DummyBox {
        return new DummyBox(
            reader.readFloat(),
            reader.readFloat(),
            Vector3Entity.decodeFromBinary(reader),
            reader.readFloat()
        );
    }
}
