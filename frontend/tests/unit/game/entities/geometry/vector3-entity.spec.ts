import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';

describe('Vector3Entity', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new Vector3Entity(0.1, 0.2, 0.3);
            const encoded = original.encodeToBinary();
            const decoded = Vector3Entity.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded.x).to.approximately(original.x, 0.000001);
            expect(decoded.y).to.approximately(original.y, 0.000001);
            expect(decoded.z).to.approximately(original.z, 0.000001);
        });
    });
});
