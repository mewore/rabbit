import { PlayerSetUpEvent } from '@/game/entities/events/player-set-up-event';
import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { expect } from 'chai';

describe('PlayerSetUpEvent', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new PlayerSetUpEvent(1, true);
            const encoded = original.encodeToBinary();
            const decoded = PlayerSetUpEvent.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
