import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class PlayerDisconnectMessage extends BinaryEntity {
    constructor(readonly playerId: number) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerDisconnectMessage {
        return new PlayerDisconnectMessage(reader.readInt());
    }
}
