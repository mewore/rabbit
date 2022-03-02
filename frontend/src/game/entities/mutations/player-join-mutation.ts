import { BinaryEntity } from '../binary-entity';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { MutationType } from './mutation-type';

export class PlayerJoinMutation extends BinaryEntity {
    constructor(readonly isReisen: boolean) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeByte(MutationType.JOIN);
        writer.writeBoolean(this.isReisen);
    }
}
