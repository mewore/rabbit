export class SignedBinaryReader {
    private index = 0;
    private readonly dataView: DataView;

    /**
     * A shorthand version of the {@link SignedBinaryReader#readInt} method.
     */
    readonly int = this.readInt.bind(this);

    constructor(data: ArrayBuffer | DataView) {
        this.dataView = data instanceof DataView ? data : new DataView(data);
    }

    withOffset(offset: number): SignedBinaryReader {
        const newReader = new SignedBinaryReader(this.dataView);
        newReader.index = this.index + offset;
        return newReader;
    }

    readNullableBoolean(): boolean | undefined {
        const byte = this.readByte();
        return byte === 0 ? false : byte === 1 ? true : undefined;
    }

    readBoolean(): boolean {
        return this.readByte() !== 0;
    }

    readByte(): number {
        return this.dataView.getInt8(this.index++);
    }

    readRemainingBytes(): ArrayBuffer {
        const length = this.dataView.byteLength - this.index;
        const result = this.dataView.buffer.slice(this.index, this.index + length);
        this.index += length;
        return result;
    }

    readInt(): number {
        const result = this.dataView.getInt32(this.index);
        this.index += 4;
        return result;
    }

    /**
     * Read a long and cast it to {@link Number}. This means that there is an assumption that the number
     * is less than 2^53 rather than less than 2^63.
     *
     * @returns The next long for this reader.
     */
    readLong(): number {
        const result = this.dataView.getBigInt64(this.index);
        this.index += 8;
        return Number(result);
    }

    readFloat(): number {
        const result = this.dataView.getFloat32(this.index);
        this.index += 4;
        return result;
    }

    readDouble(): number {
        const result = this.dataView.getFloat64(this.index);
        this.index += 8;
        return result;
    }

    readAsciiString(): string {
        const length = this.readInt();
        const buffer = this.dataView.buffer.slice(this.index, this.index + length);
        this.index += length;
        return String.fromCharCode(...new Uint8Array(buffer));
    }

    readUtf16String(): string {
        const length = this.readInt();
        const buffer = this.dataView.buffer.slice(this.index, this.index + length);
        this.index += length;
        return String.fromCharCode(...new Uint16Array(buffer));
    }

    readEntityArray<T>(type: { decodeFromBinary: (reader: SignedBinaryReader) => T }): T[] {
        return this.readArray(type.decodeFromBinary);
    }

    readArray<T>(readingFunction: (reader: SignedBinaryReader) => T): T[] {
        const count = this.readInt();
        const result = [];
        for (let i = 0; i < count; i++) {
            result.push(readingFunction(this));
        }
        return result;
    }

    readMap<K, V>(keyReader: () => K, valueReader: () => V): Map<K, V> {
        const result = new Map<K, V>();
        const size = this.readInt();
        for (let i = 0; i < size; i++) {
            result.set(keyReader(), valueReader());
        }
        return result;
    }
}
