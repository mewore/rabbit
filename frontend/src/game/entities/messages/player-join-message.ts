import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class PlayerJoinMessage extends BinaryEntity {
    constructor(
        readonly playerId: number,
        readonly username: string,
        public isReisen: boolean,
        readonly isSelf: boolean
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
        writer.writeAsciiString(this.username);
        writer.writeBoolean(this.isReisen);
        writer.writeBoolean(this.isSelf);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerJoinMessage {
        return new PlayerJoinMessage(
            reader.readInt(),
            reader.readAsciiString(),
            reader.readBoolean(),
            reader.readBoolean()
        );
    }
}
