import { BinaryEntity } from './binary-entity';
import { PlayerState } from './player-state';
import { SignedBinaryReader } from './data/signed-binary-reader';
import { SignedBinaryWriter } from './data/signed-binary-writer';

export class Player extends BinaryEntity {
    constructor(readonly id: number, readonly username: string, readonly state: PlayerState) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.id);
        writer.writeAsciiString(this.username);
        this.state.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): Player {
        return new Player(reader.readInt(), reader.readAsciiString(), PlayerState.decodeFromBinary(reader));
    }
}
