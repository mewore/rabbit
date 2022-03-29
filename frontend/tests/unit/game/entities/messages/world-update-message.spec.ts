import { describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { SignedBinaryWriter } from '@/game/entities/data/signed-binary-writer';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { WorldUpdateMessage } from '@/game/entities/messages/world-update-message';
import { PlayerControllerState } from '@/game/entities/player/player-controller-state';
import { PlayerInput } from '@/game/entities/player/player-input';
import { DummySphereState } from '@/game/entities/world/dummy-sphere-update';

describe('WorldUpdateMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const frameWriter = new SignedBinaryWriter();
            frameWriter.writeLong(1892160020551);

            const maxPlayerCount = 10;
            for (let i = 0; i < maxPlayerCount; i++) {
                new PlayerControllerState(
                    new Vector3Entity(i, i + 1, i + 2),
                    new Vector3Entity(i + 3, i + 4, i + 5),
                    i + 6,
                    i + 7
                ).appendToBinaryOutput(frameWriter);
            }
            new DummySphereState(1, new Vector3Entity(1, 2, 3), new Vector3Entity(4, 5, 6)).appendToBinaryOutput(
                frameWriter
            );

            const original = new WorldUpdateMessage(
                maxPlayerCount,
                1,
                new Map<number, number>(),
                new Map<number, PlayerInput[]>(),
                frameWriter.toArrayBuffer()
            );

            const encoded = original.encodeToBinary();
            const decoded = WorldUpdateMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).toEqual(original);
        });
    });
});
