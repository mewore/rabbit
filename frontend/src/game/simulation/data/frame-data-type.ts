export class FrameDataType {
    constructor(readonly byteSize: number) {}
}

export const BYTE = new FrameDataType(1);
export const INTEGER = new FrameDataType(4);
export const LONG = new FrameDataType(8);
export const FLOAT = new FrameDataType(4);
export const DOUBLE = new FrameDataType(8);
export const VECTOR3F = new FrameDataType(3 * FLOAT.byteSize);
