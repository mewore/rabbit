import { describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';

describe('Vector3Entity', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new Vector3Entity(0.1, 0.2, 0.3);
            const encoded = original.encodeToBinary();
            const decoded = Vector3Entity.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded.x).toBeCloseTo(original.x);
            expect(decoded.y).toBeCloseTo(original.y);
            expect(decoded.z).toBeCloseTo(original.z);
        });
    });
});
