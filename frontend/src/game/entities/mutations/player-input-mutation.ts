import { BinaryEntity } from '../binary-entity';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { PlayerInput } from '../player/player-input';
import { MutationType } from './mutation-type';

export class PlayerInputMutation extends BinaryEntity {
    constructor(private readonly input: PlayerInput) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeByte(MutationType.UPDATE);
        this.input.appendToBinaryOutput(writer);
    }
}
