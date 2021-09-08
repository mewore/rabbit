import { ForestData } from '@/game/entities/world/forest-data';
import { ForestDataMessage } from '@/game/entities/messages/forest-data-message';
import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { expect } from 'chai';

describe('ForestDataMessage', () => {
    describe('when encoded and decoded', () => {
        it('should retain its value', () => {
            const original = new ForestDataMessage(
                new ForestData(new Float32Array([1, 2, 3]), new Float32Array([4, 5, 6]), new Int8Array([0, 32, 64]))
            );
            const encoded = original.encodeToBinary();
            const decoded = ForestDataMessage.decodeFromBinary(new SignedBinaryReader(encoded));
            expect(decoded).to.deep.equals(original);
        });
    });
});
