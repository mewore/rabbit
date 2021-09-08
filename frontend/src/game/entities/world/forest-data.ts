import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class ForestData extends BinaryEntity {
    constructor(readonly plantX: Float32Array, readonly plantZ: Float32Array, readonly plantHeight: Int8Array) {
        super();
    }

    get length(): number {
        return this.plantHeight.length;
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.plantHeight.length);
        for (let i = 0; i < this.length; i++) {
            writer.writeFloat(this.plantX[i]);
            writer.writeFloat(this.plantZ[i]);
            writer.writeByte(this.plantHeight[i]);
        }
    }

    static decodeFromBinary(reader: SignedBinaryReader): ForestData {
        const length = reader.readInt();
        const plantX = new Float32Array(length);
        const plantZ = new Float32Array(length);
        const plantHeight = new Int8Array(length);
        for (let i = 0; i < plantHeight.length; i++) {
            plantX[i] = reader.readFloat();
            plantZ[i] = reader.readFloat();
            plantHeight[i] = reader.readByte();
        }
        return new ForestData(plantX, plantZ, plantHeight);
    }
}
