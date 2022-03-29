import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Vector3Entity } from '../geometry/vector3-entity';

export class PlayerControllerState extends BinaryEntity {
    constructor(
        readonly position: Vector3Entity,
        readonly motion: Vector3Entity,
        readonly groundTimeLeft: number,
        readonly jumpControlTimeLeft: number
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.position.appendToBinaryOutput(writer);
        this.motion.appendToBinaryOutput(writer);
        writer.writeFloat(this.groundTimeLeft);
        writer.writeFloat(this.jumpControlTimeLeft);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerControllerState {
        return new PlayerControllerState(
            Vector3Entity.decodeFromBinary(reader),
            Vector3Entity.decodeFromBinary(reader),
            reader.readFloat(),
            reader.readFloat()
        );
    }
}
