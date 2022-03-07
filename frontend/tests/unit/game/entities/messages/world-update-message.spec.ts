import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { WorldUpdateMessage } from '@/game/entities/messages/world-update-message';
import { PlayerState } from '@/game/entities/player-state';

describe('WorldUpdateMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new WorldUpdateMessage([
                new PlayerState(2, 3, 4, new Vector3Entity(0.1, 0.2, 0.3), new Vector3Entity(0.4, 0.5, 0.6)),
                new PlayerState(5, 6, 7, new Vector3Entity(0.4, 0.5, 0.6), new Vector3Entity(0.1, 0.2, 0.3)),
            ]);
            const encoded = original.encodeToBinary();
            const decoded = WorldUpdateMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
