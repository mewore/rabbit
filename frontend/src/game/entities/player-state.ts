import { BinaryEntity } from './binary-entity';
import { SignedBinaryReader } from './data/signed-binary-reader';
import { SignedBinaryWriter } from './data/signed-binary-writer';
import { Vector3Entity } from './geometry/vector3-entity';
import { PlayerInputMutation } from './mutations/player-input-mutation';
import { PlayerInputState } from './player-input-state';

export class PlayerState extends BinaryEntity {
    constructor(
        readonly playerId: number,
        readonly latency: number,
        readonly input: PlayerInputState,
        readonly position: Vector3Entity,
        readonly motion: Vector3Entity,
        readonly groundTimeLeft: number,
        readonly jumpControlTimeLeft: number
    ) {
        super();
    }

    get wantsToJump(): boolean {
        return PlayerInputMutation.wantsToJump(this.input.byte);
    }

    applyToTargetMotion(targetMotion: { x: number; y: number }): void {
        PlayerInputMutation.applyEncodedInputToTargetMotion(targetMotion, this.input.byte, this.input.angle);
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
        writer.writeInt(this.latency);
        this.input.appendToBinaryOutput(writer);
        this.position.appendToBinaryOutput(writer);
        this.motion.appendToBinaryOutput(writer);
        writer.writeFloat(this.groundTimeLeft);
        writer.writeFloat(this.jumpControlTimeLeft);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerState {
        return new PlayerState(
            reader.readInt(),
            reader.readInt(),
            PlayerInputState.decodeFromBinary(reader),
            Vector3Entity.decodeFromBinary(reader),
            Vector3Entity.decodeFromBinary(reader),
            reader.readFloat(),
            reader.readFloat()
        );
    }
}
