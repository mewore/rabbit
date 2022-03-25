import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { ConvexPolygonEntity } from '@/game/entities/geometry/convex-polygon-entity';
import { Vector2Entity } from '@/game/entities/geometry/vector2-entity';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { MapDataMessage } from '@/game/entities/messages/map-data-message';
import { DummyBox } from '@/game/entities/world/dummy-box';
import { MazeMap } from '@/game/entities/world/maze-map';
import { MazeWall } from '@/game/entities/world/maze-wall';

describe('MapDataMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new MapDataMessage(
                new MazeMap(
                    3,
                    3,
                    0,
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
                ),
                [new DummyBox(1, 2, new Vector3Entity(0, 0, 0), 0)]
            );
            const encoded = original.encodeToBinary();
            const decoded = MapDataMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
