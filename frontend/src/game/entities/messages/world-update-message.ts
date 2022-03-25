import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Vector3Entity } from '../geometry/vector3-entity';
import { PlayerState } from '../player-state';

export class WorldUpdateMessage extends BinaryEntity {
    constructor(
        readonly frameId: number,
        readonly playerStates: PlayerState[],
        readonly spherePositions: Vector3Entity[]
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.frameId);
        writer.writeEntityArray(this.playerStates);
        writer.writeEntityArray(this.spherePositions);
    }

    static decodeFromBinary(reader: SignedBinaryReader): WorldUpdateMessage {
        return new WorldUpdateMessage(
            reader.readInt(),
            reader.readEntityArray(PlayerState),
            reader.readEntityArray(Vector3Entity)
        );
    }
}
