import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class PlayerDisconnectEvent extends BinaryEntity {
    constructor(readonly playerId: number) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerDisconnectEvent {
        return new PlayerDisconnectEvent(reader.readInt());
    }
}
