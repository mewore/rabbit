import { BinaryEntity } from './binary-entity';
import { SignedBinaryReader } from './data/signed-binary-reader';
import { SignedBinaryWriter } from './data/signed-binary-writer';
import { Vector3Entity } from './geometry/vector3-entity';

export class PlayerState extends BinaryEntity {
    constructor(
        readonly playerId: number,
        readonly latency: number,
        readonly inputId: number,
        readonly position: Vector3Entity,
        readonly motion: Vector3Entity
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
        writer.writeInt(this.latency);
        writer.writeInt(this.inputId);
        this.position.appendToBinaryOutput(writer);
        this.motion.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerState {
        return new PlayerState(
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            Vector3Entity.decodeFromBinary(reader),
            Vector3Entity.decodeFromBinary(reader)
        );
    }
}
