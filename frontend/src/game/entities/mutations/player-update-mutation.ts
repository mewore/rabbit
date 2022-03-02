import { BinaryEntity } from '../binary-entity';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { PlayerState } from '../player-state';
import { MutationType } from './mutation-type';

export class PlayerUpdateMutation extends BinaryEntity {
    constructor(readonly newState: PlayerState) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeByte(MutationType.UPDATE);
        this.newState.appendToBinaryOutput(writer);
    }
}
