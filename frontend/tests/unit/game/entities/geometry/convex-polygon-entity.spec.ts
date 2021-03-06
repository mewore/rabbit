import { describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { ConvexPolygonEntity } from '@/game/entities/geometry/convex-polygon-entity';
import { Vector2Entity } from '@/game/entities/geometry/vector2-entity';

describe('Vector2Entity', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new ConvexPolygonEntity([
                new Vector2Entity(0, 0),
                new Vector2Entity(0, 1),
                new Vector2Entity(1, 1),
                new Vector2Entity(1, 1),
            ]);
            const encoded = original.encodeToBinary();
            const decoded = ConvexPolygonEntity.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).toEqual(original);
        });
    });
});
