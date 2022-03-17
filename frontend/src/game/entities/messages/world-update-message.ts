import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { PlayerState } from '../player-state';

export class WorldUpdateMessage extends BinaryEntity {
    constructor(readonly frameId: number, readonly playerStates: PlayerState[]) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.frameId);
        writer.writeEntityArray(this.playerStates);
    }

    static decodeFromBinary(reader: SignedBinaryReader): WorldUpdateMessage {
        return new WorldUpdateMessage(reader.readInt(), reader.readEntityArray(PlayerState));
    }
}
