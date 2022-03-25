import { BinaryEntity } from './binary-entity';
import { SignedBinaryReader } from './data/signed-binary-reader';
import { SignedBinaryWriter } from './data/signed-binary-writer';
import { PlayerInputMutation } from './mutations/player-input-mutation';

export class PlayerInputState extends BinaryEntity {
    constructor(readonly id: number, readonly byte: number, readonly angle: number) {
        super();
    }

    get wantsToJump(): boolean {
        return PlayerInputMutation.wantsToJump(this.byte);
    }

    applyToTargetMotion(targetMotion: { x: number; y: number }): void {
        PlayerInputMutation.applyEncodedInputToTargetMotion(targetMotion, this.byte, this.angle);
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.id);
        writer.writeByte(this.byte);
        writer.writeFloat(this.angle);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerInputState {
        return new PlayerInputState(reader.readInt(), reader.readByte(), reader.readFloat());
    }
}
