import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Vector3Entity } from '../geometry/vector3-entity';
import { PlayerInputState } from '../player-input-state';
import { PlayerState } from '../player-state';
import { DummySphereUpdate } from '../world/dummy-sphere-update';

export class WorldUpdateMessage extends BinaryEntity {
    constructor(
        readonly frameId: number,
        readonly playerStates: PlayerState[],
        readonly spheres: DummySphereUpdate[],
        private readonly maxPlayerCount: number,
        private readonly intArraySize: number
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.playerStates.length);
        const playerStatebyId = new Map<number, PlayerState>();
        for (const playerState of this.playerStates) {
            playerStatebyId.set(playerState.playerId, playerState);
            writer.writeInt(playerState.playerId);
            writer.writeInt(playerState.latency);
        }

        writer.writeInt(this.maxPlayerCount);
        writer.writeInt(this.spheres.length);
        writer.writeInt(this.intArraySize);

        writer.writeInt(this.frameId);
        const floatArray = [];
        for (let i = 0; i < this.maxPlayerCount; i++) {
            writer.writeInt(0);
            const playerState = playerStatebyId.get(i);
            if (playerState) {
                writer.writeInt(playerState.input.id);
                writer.writeInt(playerState.input.byte);
                floatArray.push(playerState.input.angle);
                floatArray.push(playerState.position.x, playerState.position.y, playerState.position.z);
                floatArray.push(playerState.motion.x, playerState.motion.y, playerState.motion.z);
                floatArray.push(playerState.groundTimeLeft);
                floatArray.push(playerState.jumpControlTimeLeft);
            } else {
                writer.writeInt(0);
                writer.writeInt(0);
                floatArray.push(0);
                floatArray.push(0, 0, 0);
                floatArray.push(0, 0, 0);
                floatArray.push(0);
                floatArray.push(0);
            }
        }

        for (const sphere of this.spheres) {
            writer.writeInt(sphere.activationState);
            floatArray.push(sphere.position.x, sphere.position.y, sphere.position.z);
            floatArray.push(sphere.motion.x, sphere.motion.y, sphere.motion.z);
        }

        // There shouldn't be a mismatch between the written integers and the integer array size.
        // ...but if there is one, pad with zeroes.
        const intsWritten = 1 + this.maxPlayerCount * 3 + this.spheres.length;
        for (let i = intsWritten; i < this.intArraySize; i++) {
            writer.writeInt(0);
        }
        for (const float of floatArray) {
            writer.writeFloat(float);
        }
    }

    static decodeFromBinary(reader: SignedBinaryReader): WorldUpdateMessage {
        const currentPlayerCount = reader.readInt();
        const playerLatencyById = new Map<number, number>();
        for (let i = 0; i < currentPlayerCount; i++) {
            playerLatencyById.set(reader.readInt(), reader.readInt());
        }

        const maxPlayerCount = reader.readInt();
        const sphereCount = reader.readInt();
        const intArraySize = reader.readInt();

        const intReader = reader;
        const int = () => intReader.readInt();

        const floatReader = reader.withOffset(intArraySize * 4);
        const float = () => floatReader.readFloat();
        const vector3 = () => Vector3Entity.decodeFromBinary(floatReader);

        const frameId = int();
        const playerStates: PlayerState[] = [];
        for (let i = 0; i < maxPlayerCount; i++) {
            const latency = playerLatencyById.get(i);
            // Player UID - not used
            int();
            const playerState = new PlayerState(
                i,
                latency || 0,
                new PlayerInputState(int(), int(), float()),
                vector3(),
                vector3(),
                float(),
                float()
            );
            if (latency != null) {
                playerStates.push(playerState);
            }
        }

        const sphereStates: DummySphereUpdate[] = [];
        for (let i = 0; i < sphereCount; i++) {
            sphereStates.push(new DummySphereUpdate(int(), vector3(), vector3()));
        }

        return new WorldUpdateMessage(frameId, playerStates, sphereStates, maxPlayerCount, intArraySize);
    }
}
