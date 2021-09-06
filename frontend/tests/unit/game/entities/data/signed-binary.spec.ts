import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { SignedBinaryWriter } from '@/game/entities/data/signed-binary-writer';
import { expect } from 'chai';

describe('SignedBinaryReader and SignedBinaryWriter', () => {
    let writer: SignedBinaryWriter;

    beforeEach(() => {
        writer = new SignedBinaryWriter();
    });

    describe('when encoding and decoding a byte', () => {
        it('should retain only the byte part and make it signed', () => {
            writer.writeByte((1 << 10) + 200);
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readByte()).to.equal(200 - 256);
        });
    });

    describe('when encoding and decoding an integer', () => {
        it('should retain only the integer part', () => {
            writer.writeInt(1024 * 1024 * 1024 * 1024 + 420);
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readInt()).to.equal(420);
        });
    });

    describe('when encoding and decoding a double', () => {
        it('should retain only the byte part', () => {
            writer.writeDouble(0.123);
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readDouble()).to.equal(0.123);
        });
    });

    describe('when encoding and decoding an ASCII string', () => {
        it('should retain its value', () => {
            writer.writeAsciiString('Test');
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readAsciiString()).to.equal('Test');
        });

        describe('when the string is with non-ASCII characters', () => {
            it('should throw an error', () => {
                expect(() => writer.writeAsciiString('Non-ASCII character: ⚠')).to.throw(
                    '"Non-ASCII character: ⚠" is not an ASCII string! ' +
                        'It contains a non-ASCII character "⚠" at index 21'
                );
            });
        });
    });

    describe('when encoding and decoding a UTF-16 string', () => {
        it('should retain its value', () => {
            writer.writeUtf16String('Warning: ⚠');
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readUtf16String()).to.equal('Warning: ⚠');
        });
    });
});
