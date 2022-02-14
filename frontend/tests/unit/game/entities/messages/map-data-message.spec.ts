import { ConvexPolygonEntity } from '@/game/entities/geometry/convex-polygon-entity';
import { MapDataMessage } from '@/game/entities/messages/map-data-message';
import { MazeMap } from '@/game/entities/world/maze-map';
import { MazeWall } from '@/game/entities/world/maze-wall';
import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector2Entity } from '@/game/entities/geometry/vector2-entity';
import { expect } from 'chai';

describe('MapDataMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new MapDataMessage(
                new MazeMap(
                    3,
                    3,
                    [
                        [true, false, false],
                        [true, true, false],
                        [true, false, false],
                    ],
                    [
                        new MazeWall(
                            0,
                            0,
                            2,
                            0,
                            new ConvexPolygonEntity([
                                new Vector2Entity(0, 0),
                                new Vector2Entity(0, 1),
                                new Vector2Entity(1, 1),
                                new Vector2Entity(1, 1),
                            ])
                        ),
                    ]
                )
            );
            const encoded = original.encodeToBinary();
            const decoded = MapDataMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
