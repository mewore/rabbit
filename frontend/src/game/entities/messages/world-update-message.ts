import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { PlayerControllerState } from '../player/player-controller-state';
import { PlayerInput } from '../player/player-input';
import { PlayerState } from '../player-state';
import { DummySphereState } from '../world/dummy-sphere-update';

export class WorldUpdateMessage extends BinaryEntity {
    readonly frameId: number;
    readonly playerStates: PlayerState[] = [];
    readonly spheres: DummySphereState[] = [];

    constructor(
        private readonly maxPlayerCount: number,
        private readonly sphereCount: number,
        private readonly playerLatencyById: Map<number, number>,
        readonly newPlayerInputs: Map<number, PlayerInput[]>,
        private readonly frame: ArrayBuffer
    ) {
        super();

        const reader = new SignedBinaryReader(frame);
        this.frameId = reader.readLong();

        for (let i = 0; i < maxPlayerCount; i++) {
            const latency = playerLatencyById.get(i);
            const playerState = new PlayerState(i, latency || 0, PlayerControllerState.decodeFromBinary(reader));
            if (latency != null) {
                this.playerStates.push(playerState);
            }
        }

        for (let i = 0; i < sphereCount; i++) {
            this.spheres.push(DummySphereState.decodeFromBinary(reader));
        }
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.maxPlayerCount);
        writer.writeInt(this.sphereCount);

        writer.writeMap(this.playerLatencyById, writer.int, writer.int);
        writer.writeMap(this.newPlayerInputs, writer.int, writer.writeEntityArray.bind(writer));

        for (const value of new Uint8Array(this.frame)) {
            writer.writeByte(value);
        }
    }

    static decodeFromBinary(reader: SignedBinaryReader): WorldUpdateMessage {
        const maxPlayerCount = reader.readInt();
        const sphereCount = reader.readInt();

        const playerLatencyById = reader.readMap(reader.int, reader.int);
        const playerInputs = reader.readMap(reader.int, () => reader.readEntityArray(PlayerInput));

        const frame = reader.readRemainingBytes();
        return new WorldUpdateMessage(maxPlayerCount, sphereCount, playerLatencyById, playerInputs, frame);
    }
}
