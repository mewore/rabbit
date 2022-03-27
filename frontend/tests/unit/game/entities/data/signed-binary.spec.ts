import { beforeEach, describe, expect, it } from '@jest/globals';

import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { SignedBinaryWriter } from '@/game/entities/data/signed-binary-writer';

import { TestEntity } from './test-entity';

describe('SignedBinaryReader and SignedBinaryWriter', () => {
    let writer: SignedBinaryWriter;

    beforeEach(() => {
        writer = new SignedBinaryWriter();
    });

    describe('SignedBinaryReader#withOffset', () => {
        it('should return an independent instance with an offset', () => {
            writer.writeInt(1);
            writer.writeInt(2);
            writer.writeInt(3);
            writer.writeInt(4);
            const reader = new SignedBinaryReader(writer.toArrayBuffer());
            reader.readInt();
            expect(reader.withOffset(2 * 4).readInt()).toBe(4);
            expect(reader.readInt()).toBe(2);
        });
    });

    describe('when encoding and decoding a boolean', () => {
        describe('when the boolean is true', () => {
            it('should retain its value', () => {
                writer.writeBoolean(true);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readBoolean()).toBe(true);
            });
        });
        describe('when the boolean is false', () => {
            it('should retain its value', () => {
                writer.writeBoolean(false);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readBoolean()).toBe(false);
            });
        });
    });

    describe('when decoding a nullable boolean', () => {
        describe('when the boolean is true', () => {
            it('should retain its value', () => {
                writer.writeNullableBoolean(true);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readNullableBoolean()).toBe(true);
            });
        });
        describe('when the boolean is false', () => {
            it('should retain its value', () => {
                writer.writeNullableBoolean(false);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readNullableBoolean()).toBe(false);
            });
        });
        describe('when the boolean is undefined', () => {
            it('should be undefined', () => {
                writer.writeNullableBoolean(undefined);
                expect(new SignedBinaryReader(writer.toArrayBuffer()).readNullableBoolean()).toBeUndefined();
            });
        });
    });

    describe('when encoding and decoding a byte', () => {
        it('should retain only the byte part and make it signed', () => {
            writer.writeByte((1 << 10) + 200);
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readByte()).toBe(200 - 256);
        });
    });

    describe('when encoding and decoding an integer', () => {
        it('should retain only the integer part', () => {
            writer.writeInt(1024 * 1024 * 1024 * 1024 + 420);
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readInt()).toBe(420);
        });
    });

    describe('when encoding and decoding a float', () => {
        it('should retain only the float part', () => {
            writer.writeFloat(0.123);
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readFloat()).toBeCloseTo(0.123);
        });
    });

    describe('when encoding and decoding a double', () => {
        it('should retain only the double part', () => {
            writer.writeDouble(0.123);
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readDouble()).toBe(0.123);
        });
    });

    describe('when encoding and decoding an ASCII string', () => {
        it('should retain its value', () => {
            writer.writeAsciiString('Test');
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readAsciiString()).toBe('Test');
        });

        describe('when the string is with non-ASCII characters', () => {
            it('should throw an error', () => {
                expect(() => writer.writeAsciiString('Non-ASCII character: ⚠')).toThrow(
                    '"Non-ASCII character: ⚠" is not an ASCII string! ' +
                        'It contains a non-ASCII character "⚠" at index 21'
                );
            });
        });
    });

    describe('when encoding and decoding a UTF-16 string', () => {
        it('should retain its value', () => {
            writer.writeUtf16String('Warning: ⚠');
            expect(new SignedBinaryReader(writer.toArrayBuffer()).readUtf16String()).toBe('Warning: ⚠');
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
            ).toBe('1, 2');
        });
    });
});
