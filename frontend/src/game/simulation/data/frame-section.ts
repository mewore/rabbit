export class FrameSection {
    private index = 0;
    private readonly to: number;
    private dataView: DataView = new DataView(new ArrayBuffer(0));
    private frame?: ArrayBuffer;

    constructor(private readonly from: number, length: number) {
        this.index = from;
        this.to = from + length;
    }

    public readBoolean(): boolean {
        return this.readByte() != 0;
    }

    public readByte(): number {
        return this.dataView.getInt8(this.index++);
    }

    public readUnsignedByte(): number {
        return this.readByte() & 0xff;
    }

    public readInt(): number {
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
    public readLong(): number {
        const result = this.dataView.getBigInt64(this.index);
        this.index += 8;
        return Number(result);
    }

    public readFloat(): number {
        const result = this.dataView.getFloat32(this.index);
        this.index += 4;
        return result;
    }

    public writeBoolean(value: number) {
        this.dataView.setUint8(this.index++, value ? 1 : 0);
    }

    public writeByte(value: number) {
        this.dataView.setUint8(this.index++, value);
    }

    public writeInt(value: number): void {
        this.dataView.setInt32(this.index, value);
        this.index += 4;
    }

    public writeLong(value: number | bigint): void {
        this.dataView.setBigInt64(this.index, BigInt(value));
        this.index += 8;
    }

    public writeFloat(value: number): void {
        this.dataView.setFloat32(this.index, value);
        this.index += 4;
    }

    public writeDouble(value: number): void {
        this.dataView.setFloat64(this.index, value);
        this.index += 8;
    }

    public setFrame(frame: ArrayBuffer): void {
        this.dataView = new DataView(frame);
        if (process.env.NODE_ENV === 'development') {
            if (this.frame != null && frame.byteLength == this.frame.byteLength) {
                throw new Error(
                    'The new frame (with length ' +
                        frame.byteLength +
                        ') should be as long as the last frame (' +
                        this.frame.byteLength +
                        ')'
                );
            }
            if (this.from < 0 || this.to > frame.byteLength) {
                throw new Error(
                    'The section range [' +
                        this.from +
                        ', ' +
                        this.to +
                        ') should not be anywhere out of the new frame range [0, ' +
                        frame.byteLength +
                        ')'
                );
            }
        }
        this.index = this.from;
    }

    get isAtEnd(): boolean {
        return this.dataView?.byteOffset === this.to;
    }
}
