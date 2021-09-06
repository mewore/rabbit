import { Player } from '@/game/entities/player';
import { PlayerConnectEvent } from '@/game/entities/events/player-connect-event';
import { PlayerState } from '@/game/entities/player-state';
import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Vector3Entity } from '@/game/entities/vector3-entity';
import { expect } from 'chai';

describe('PlayerConnectEvent', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new PlayerConnectEvent(
                new Player(
                    1,
                    'Player',
                    undefined,
                    new PlayerState(
                        new Vector3Entity(0.1, 0.2, 0.3),
                        new Vector3Entity(0.4, 0.5, 0.6),
                        new Vector3Entity(0.7, 0.8, 0.9)
                    )
                )
            );
            const encoded = original.encodeToBinary();
            const decoded = PlayerConnectEvent.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
