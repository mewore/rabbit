import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { PlayerState } from '../player-state';
import { DummySphereUpdate } from '../world/dummy-sphere-update';

export class WorldUpdateMessage extends BinaryEntity {
    constructor(readonly frameId: number, readonly playerStates: PlayerState[], readonly spheres: DummySphereUpdate[]) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.frameId);
        writer.writeEntityArray(this.playerStates);
        writer.writeEntityArray(this.spheres);
    }

    static decodeFromBinary(reader: SignedBinaryReader): WorldUpdateMessage {
        return new WorldUpdateMessage(
            reader.readInt(),
            reader.readEntityArray(PlayerState),
            reader.readEntityArray(DummySphereUpdate)
        );
    }
}
