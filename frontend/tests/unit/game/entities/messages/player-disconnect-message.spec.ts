import { describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { PlayerDisconnectMessage } from '@/game/entities/messages/player-disconnect-message';

describe('PlayerDisconnectMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new PlayerDisconnectMessage(1);
            const encoded = original.encodeToBinary();
            const decoded = PlayerDisconnectMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).toEqual(original);
        });
    });
});
