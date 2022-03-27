import { describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { Player } from '@/game/entities/player';

describe('Player', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new Player(18, 'Player', true, 1);
            const encoded = original.encodeToBinary();
            const decoded = Player.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).toEqual(original);
        });
    });
});
