import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { PlayerState } from '@/game/entities/player-state';

describe('PlayerState', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new PlayerState(
                1,
                1,
                1,
                new Vector3Entity(0.1, 0.2, 0.3),
                new Vector3Entity(0.4, 0.5, 0.6)
            );
            const encoded = original.encodeToBinary();
            const decoded = PlayerState.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
