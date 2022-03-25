import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { DummySphereUpdate } from '@/game/entities/world/dummy-sphere-update';

describe('DummySphereUpdate', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new DummySphereUpdate(1, new Vector3Entity(0, 0, 0), new Vector3Entity(0, 0, 0));
            const encoded = original.encodeToBinary();
            const decoded = DummySphereUpdate.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });

        it('should roughly retain its floating point values', () => {
            const original = new DummySphereUpdate(1, new Vector3Entity(1, 2, 3), new Vector3Entity(4, 5, 6));
            const encoded = original.encodeToBinary();
            const decoded = DummySphereUpdate.decodeFromBinary(new SignedBinaryReader(encoded));

            expect(decoded.position.x).to.approximately(original.position.x, 0.000001);
            expect(decoded.motion.x).to.approximately(original.motion.x, 0.000001);
        });
    });
});
