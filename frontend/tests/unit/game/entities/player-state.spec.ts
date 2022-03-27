import { describe, expect, it } from '@jest/globals';

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
            expect(decoded).toEqual(original);
        });

        it('should roughly retain its floating point values', () => {
            const original = new PlayerState(
                1,
                1,
                new PlayerInputState(1, 0, 2.4),
                new Vector3Entity(0.1, 0.2, 0.3),
                new Vector3Entity(0.4, 0.5, 0.6),
                25.25,
                12.34
            );
            const encoded = original.encodeToBinary();
            const decoded = PlayerState.decodeFromBinary(new SignedBinaryReader(encoded));

            expect(decoded.input.angle).toBeCloseTo(original.input.angle);
            expect(decoded.groundTimeLeft).toBeCloseTo(original.groundTimeLeft);
            expect(decoded.jumpControlTimeLeft).toBeCloseTo(original.jumpControlTimeLeft);
            expect(decoded.position.x).toBeCloseTo(original.position.x);
            expect(decoded.motion.x).toBeCloseTo(original.motion.x);
        });
    });
});
