import { expect } from 'chai';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector2Entity } from '@/game/entities/geometry/vector2-entity';
import { Vector3Entity } from '@/game/entities/geometry/vector3-entity';
import { PlayerUpdateMessage } from '@/game/entities/messages/player-update-message';
import { PlayerState } from '@/game/entities/player-state';

describe('PlayerUpdateMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new PlayerUpdateMessage(
                1,
                new PlayerState(
                    new Vector3Entity(0.1, 0.2, 0.3),
                    new Vector3Entity(0.4, 0.5, 0.6),
                    new Vector2Entity(0.7, 0.8)
                )
            );
            const encoded = original.encodeToBinary();
            const decoded = PlayerUpdateMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
