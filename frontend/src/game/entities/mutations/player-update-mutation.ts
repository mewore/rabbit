import { BinaryEntity } from '../binary-entity';
import { MutationType } from './mutation-type';
import { PlayerState } from '../player-state';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class PlayerUpdateMutation extends BinaryEntity {
    constructor(readonly newState: PlayerState) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeByte(MutationType.UPDATE);
        this.newState.appendToBinaryOutput(writer);
    }
}
