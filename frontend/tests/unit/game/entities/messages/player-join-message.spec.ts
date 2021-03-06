import { describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { PlayerJoinMessage } from '@/game/entities/messages/player-join-message';

describe('PlayerJoinMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new PlayerJoinMessage(1, 'Player', true, false);
            const encoded = original.encodeToBinary();
            const decoded = PlayerJoinMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).toEqual(original);
        });
    });
});
