import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class PlayerSetUpEvent extends BinaryEntity {
    constructor(readonly playerId: number, readonly isReisen: boolean) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
        writer.writeBoolean(this.isReisen);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerSetUpEvent {
        return new PlayerSetUpEvent(reader.readInt(), reader.readBoolean());
    }
}
