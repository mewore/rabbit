import { SignedBinaryWriter } from './data/signed-binary-writer';

export abstract class BinaryEntity {
    encodeToBinary(): ArrayBuffer {
        const binaryWriter = new SignedBinaryWriter();
        this.appendToBinaryOutput(binaryWriter);
        return binaryWriter.toArrayBuffer();
    }

    abstract appendToBinaryOutput(writer: SignedBinaryWriter): void;
}
