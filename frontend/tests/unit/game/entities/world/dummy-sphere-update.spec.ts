import { describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { DummySphereUpdate } from '@/game/entities/world/dummy-sphere-update';

describe('DummySphereUpdate', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new DummySphereUpdate(1, new Vector3Entity(0, 0, 0), new Vector3Entity(0, 0, 0));
            const encoded = original.encodeToBinary();
            const decoded = DummySphereUpdate.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).toEqual(original);
        });

        it('should roughly retain its floating point values', () => {
            const original = new DummySphereUpdate(1, new Vector3Entity(1, 2, 3), new Vector3Entity(4, 5, 6));
            const encoded = original.encodeToBinary();
            const decoded = DummySphereUpdate.decodeFromBinary(new SignedBinaryReader(encoded));

            expect(decoded.position.x).toBeCloseTo(original.position.x);
            expect(decoded.motion.x).toBeCloseTo(original.motion.x);
        });
    });
});
