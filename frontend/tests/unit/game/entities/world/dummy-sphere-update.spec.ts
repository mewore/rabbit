import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { WorldUpdateMessage } from '@/game/entities/messages/world-update-message';
import { DummySphereUpdate } from '@/game/entities/world/dummy-sphere-update';

describe('DummySphereUpdate', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new DummySphereUpdate(new Vector3Entity(0.1, 0.2, 0.3), new Vector3Entity(0.1, 0.2, 0.3));
            const encoded = original.encodeToBinary();
            const decoded = WorldUpdateMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
