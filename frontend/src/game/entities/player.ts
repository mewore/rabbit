import { BinaryEntity } from './binary-entity';
import { SignedBinaryReader } from './data/signed-binary-reader';
import { SignedBinaryWriter } from './data/signed-binary-writer';

export class Player extends BinaryEntity {
    constructor(readonly id: number, readonly username: string, public isReisen: boolean, readonly latency: number) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.id);
        writer.writeAsciiString(this.username);
        writer.writeBoolean(this.isReisen);
        writer.writeInt(this.latency);
    }

    static decodeFromBinary(reader: SignedBinaryReader): Player {
        return new Player(reader.readInt(), reader.readAsciiString(), reader.readBoolean(), reader.readInt());
    }
}
