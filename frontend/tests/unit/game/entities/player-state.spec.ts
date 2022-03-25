import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { PlayerInputState } from '@/game/entities/player-input-state';
import { PlayerState } from '@/game/entities/player-state';

describe('PlayerState', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new PlayerState(
                1,
                1,
                new PlayerInputState(1, 0, 0),
                new Vector3Entity(0, 0, 0),
                new Vector3Entity(0, 0, 0),
                0,
                0
            );
            const encoded = original.encodeToBinary();
            const decoded = PlayerState.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });

        it('should roughly retain its floating point values', () => {
            const original = new PlayerState(
                1,
                1,
                new PlayerInputState(1, 0, 2.4),
                new Vector3Entity(0.1, 0.2, 0.3),
                new Vector3Entity(0.4, 0.5, 0.6),
                25.25,
                25.25
            );
            const encoded = original.encodeToBinary();
            const decoded = PlayerState.decodeFromBinary(new SignedBinaryReader(encoded));

            expect(decoded.input.angle).to.approximately(original.input.angle, 0.000001);
            expect(decoded.groundTimeLeft).to.approximately(original.groundTimeLeft, 0.000001);
            expect(decoded.jumpControlTimeLeft).to.approximately(original.jumpControlTimeLeft, 0.000001);
            expect(decoded.position.x).to.approximately(original.position.x, 0.000001);
            expect(decoded.motion.x).to.approximately(original.motion.x, 0.000001);
        });
    });
});
