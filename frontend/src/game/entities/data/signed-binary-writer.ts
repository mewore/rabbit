enum NumberBinaryType {
    BYTE,
    INTEGER,
    DOUBLE,
}

enum StringBinaryType {
    US_ASCII,
    UTF16,
}

type BinaryData = [number, NumberBinaryType] | Uint8Array;

export class SignedBinaryWriter {
    private readonly data: BinaryData[] = [];

    writeByte(value: number): void {
        this.data.push([value, NumberBinaryType.BYTE]);
    }

    writeInt(value: number): void {
        this.data.push([value, NumberBinaryType.INTEGER]);
    }

    writeDouble(value: number): void {
        this.data.push([value, NumberBinaryType.DOUBLE]);
    }

    writeAsciiString(value: string): void {
        for (let i = 0; i < value.length; i++) {
            if (value.charCodeAt(i) >= 128) {
                throw new Error(
                    `"${value}" is not an ASCII string! ` +
                        `It contains a non-ASCII character "${value.charAt(i)}" at index ${i}`
                );
            }
        }
        this.data.push([value.length, NumberBinaryType.INTEGER]);
        this.data.push(SignedBinaryWriter.encodeString(value, StringBinaryType.US_ASCII));
    }

    writeUtf16String(value: string): void {
        this.data.push([value.length * 2, NumberBinaryType.INTEGER]);
        this.data.push(SignedBinaryWriter.encodeString(value, StringBinaryType.UTF16));
    }

    toArrayBuffer(): ArrayBuffer {
        let size = 0;
        for (const dataPart of this.data) {
            size += SignedBinaryWriter.getDataPartLength(dataPart);
        }
        let index = 0;
        const dataView = new DataView(new ArrayBuffer(size));
        for (const dataPart of this.data) {
            index = SignedBinaryWriter.updateDataView(index, dataPart, dataView);
        }
        if (index != size) {
            throw new Error(`The index (${index}) does not equal the data size (${size}) after writing all the data`);
        }
        return dataView.buffer;
    }

    private static updateDataView(index: number, dataPart: BinaryData, dataView: DataView): number {
        if (dataPart instanceof Uint8Array) {
            new Uint8Array(dataView.buffer).set(dataPart, index);
            return index + dataPart.byteLength;
        }
        const value = dataPart[0];
        const type = dataPart[1];
        switch (type) {
            case NumberBinaryType.BYTE:
                dataView.setInt8(index, value);
                return index + 1;
            case NumberBinaryType.INTEGER:
                dataView.setInt32(index, value);
                return index + 4;
            case NumberBinaryType.DOUBLE:
                dataView.setFloat64(index, value);
                return index + 8;
        }
    }

    private static encodeString(value: string, type: StringBinaryType): Uint8Array {
        // NOTE: JS strings are encoded in big-endian UTF-16. Characters that are represented with two 16-bit
        //  parts are considered as two separate characters, which makes encoding much easier.
        const buffer = new ArrayBuffer(type === StringBinaryType.UTF16 ? value.length * 2 : value.length);
        const encodingView = type === StringBinaryType.UTF16 ? new Uint16Array(buffer) : new Uint8Array(buffer);
        for (let i = 0; i < value.length; i++) {
            encodingView[i] = value.charCodeAt(i);
        }
        return new Uint8Array(buffer);
    }

    private static getDataPartLength(dataPart: BinaryData): number {
        if (dataPart instanceof Uint8Array) {
            return dataPart.byteLength;
        }
        switch (dataPart[1]) {
            case NumberBinaryType.BYTE:
                return 1;
            case NumberBinaryType.INTEGER:
                return 4;
            case NumberBinaryType.DOUBLE:
                return 8;
        }
    }
}
