import { describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { WorldUpdateMessage } from '@/game/entities/messages/world-update-message';
import { PlayerInputState } from '@/game/entities/player-input-state';
import { PlayerState } from '@/game/entities/player-state';
import { DummySphereUpdate } from '@/game/entities/world/dummy-sphere-update';

describe('WorldUpdateMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new WorldUpdateMessage(
                123,
                [
                    new PlayerState(
                        2,
                        3,
                        new PlayerInputState(4, 0, 0),
                        new Vector3Entity(0, 0, 0),
                        new Vector3Entity(0, 0, 0),
                        0,
                        0
                    ),
                    new PlayerState(
                        5,
                        6,
                        new PlayerInputState(7, 0, 0),
                        new Vector3Entity(0, 0, 0),
                        new Vector3Entity(0, 0, 0),
                        0,
                        0
                    ),
                ],
                [new DummySphereUpdate(1, new Vector3Entity(0, 0, 0), new Vector3Entity(0, 0, 0))],
                10,
                100
            );
            const encoded = original.encodeToBinary();
            const decoded = WorldUpdateMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).toEqual(original);
        });
    });
});
