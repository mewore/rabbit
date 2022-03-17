import { BinaryEntity } from './binary-entity';
import { SignedBinaryReader } from './data/signed-binary-reader';
import { SignedBinaryWriter } from './data/signed-binary-writer';
import { Vector3Entity } from './geometry/vector3-entity';
import { PlayerInputMutation } from './mutations/player-input-mutation';

export class PlayerState extends BinaryEntity {
    constructor(
        readonly playerId: number,
        readonly latency: number,
        readonly inputId: number,
        readonly inputByte: number,
        readonly inputAngle: number,
        readonly position: Vector3Entity,
        readonly motion: Vector3Entity
    ) {
        super();
    }

    get wantsToJump(): boolean {
        return PlayerInputMutation.wantsToJump(this.inputByte);
    }

    applyToTargetMotion(targetMotion: { x: number; y: number }): void {
        PlayerInputMutation.applyEncodedInputToTargetMotion(targetMotion, this.inputByte, this.inputAngle);
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
        writer.writeInt(this.latency);
        writer.writeInt(this.inputId);
        writer.writeByte(this.inputByte);
        writer.writeFloat(this.inputAngle);
        this.position.appendToBinaryOutput(writer);
        this.motion.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerState {
        return new PlayerState(
            reader.readInt(),
            reader.readInt(),
            reader.readInt(),
            reader.readByte(),
            reader.readFloat(),
            Vector3Entity.decodeFromBinary(reader),
            Vector3Entity.decodeFromBinary(reader)
        );
    }
}
