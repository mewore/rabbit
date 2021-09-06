import { PlayerDisconnectEvent } from '@/game/entities/events/player-disconnect-event';
import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { expect } from 'chai';

describe('PlayerDisconnectEvent', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new PlayerDisconnectEvent(1);
            const encoded = original.encodeToBinary();
            const decoded = PlayerDisconnectEvent.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
