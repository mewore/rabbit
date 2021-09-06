import { BinaryEntity } from '../binary-entity';
import { PlayerState } from '../player-state';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class PlayerUpdateEvent extends BinaryEntity {
    constructor(readonly playerId: number, readonly newState: PlayerState) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerId);
        this.newState.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerUpdateEvent {
        return new PlayerUpdateEvent(reader.readInt(), PlayerState.decodeFromBinary(reader));
    }
}
