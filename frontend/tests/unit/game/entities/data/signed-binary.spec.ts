import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { SignedBinaryWriter } from '@/game/entities/data/signed-binary-writer';
import { TestEntity } from './test-entity';
import { expect } from 'chai';

describe('SignedBinaryReader and SignedBinaryWriter', () => {
    let writer: SignedBinaryWriter;

    beforeEach(() => {
        writer = new SignedBinaryWriter();
    });

    describe('when encoding and decoding a boolean', () => {
        describe('when the boolean is true', () => {
            it('should retain its value', () => {
                writer.writeBoolean(true);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readBoolean()).to.be.true;
            });
        });
        describe('when the boolean is false', () => {
            it('should retain its value', () => {
                writer.writeBoolean(false);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readBoolean()).to.be.false;
            });
        });
    });

    describe('when decoding a nullable boolean', () => {
        describe('when the boolean is true', () => {
            it('should retain its value', () => {
                writer.writeNullableBoolean(true);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readNullableBoolean()).to.be.true;
            });
        });
        describe('when the boolean is false', () => {
            it('should retain its value', () => {
                writer.writeNullableBoolean(false);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readNullableBoolean()).to.be.false;
            });
        });
        describe('when the boolean is undefined', () => {
            it('should be undefined', () => {
                writer.writeNullableBoolean(undefined);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readNullableBoolean()).to.be.undefined;
            });
        });
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

    describe('when encoding and decoding a float', () => {
        it('should retain only the float part', () => {
            writer.writeFloat(0.123);
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readFloat()).to.closeTo(0.123, 0.00001);
        });
    });

    describe('when encoding and decoding a double', () => {
        it('should retain only the double part', () => {
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

    describe('when encoding and decoding an array of entities', () => {
        it('should retain its value', () => {
            writer.writeEntityArray([new TestEntity(1), new TestEntity(2)]);
            expect(
                new SignedBinaryReader(writer.toArrayBuffer())
                    .readEntityArray(TestEntity)
                    .map((entity: TestEntity) => entity.data)
                    .join(', ')
            ).to.equal('1, 2');
        });
    });
});
