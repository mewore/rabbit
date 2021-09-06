export class SignedBinaryReader {
    private index = 0;
    private readonly dataView: DataView;

    constructor(data: ArrayBuffer) {
        this.dataView = new DataView(data);
    }

    readByte(): number {
        return this.dataView.getInt8(this.index++);
    }

    readInt(): number {
        const result = this.dataView.getInt32(this.index);
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
}
