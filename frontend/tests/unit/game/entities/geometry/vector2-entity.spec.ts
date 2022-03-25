import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector2Entity } from '@/game/entities/geometry/vector2-entity';

describe('Vector2Entity', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new Vector2Entity(0.1, 0.2);
            const encoded = original.encodeToBinary();
            const decoded = Vector2Entity.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded.x).to.approximately(original.x, 0.000001);
            expect(decoded.y).to.approximately(original.y, 0.000001);
        });
    });
});
