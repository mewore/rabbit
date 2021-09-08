import { BinaryEntity } from '../binary-entity';
import { PlayerState } from '../player-state';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class PlayerUpdateMessage extends BinaryEntity {
    constructor(readonly playerId: number, readonly newState: PlayerState) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
        this.newState.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerUpdateMessage {
        return new PlayerUpdateMessage(reader.readInt(), PlayerState.decodeFromBinary(reader));
    }
}