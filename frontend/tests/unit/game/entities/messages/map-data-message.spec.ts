import { ConvexPolygonEntity } from '@/game/entities/geometry/convex-polygon-entity';
import { ForestData } from '@/game/entities/world/forest-data';
import { MapData } from '@/game/entities/world/map-data';
import { MapDataMessage } from '@/game/entities/messages/map-data-message';
import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector2Entity } from '@/game/entities/geometry/vector2-entity';
import { expect } from 'chai';

describe('MapDataMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new MapDataMessage(
                new MapData(
                    [
                        [true, false, false],
                        [true, true, false],
                        [true, false, false],
                    ],
                    [
                        new ConvexPolygonEntity([
                            new Vector2Entity(0, 0),
                            new Vector2Entity(0, 1),
                            new Vector2Entity(1, 1),
                            new Vector2Entity(1, 1),
                        ]),
                    ]
                ),
                new ForestData(new Float32Array([1, 2, 3]), new Float32Array([4, 5, 6]), new Int8Array([0, 32, 64]))
            );
            const encoded = original.encodeToBinary();
            const decoded = MapDataMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
