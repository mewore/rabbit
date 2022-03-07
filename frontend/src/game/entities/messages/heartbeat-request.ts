import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class HeartbeatRequest extends BinaryEntity {
    constructor(readonly id: number) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.id);
    }

    static decodeFromBinary(reader: SignedBinaryReader): HeartbeatRequest {
        return new HeartbeatRequest(reader.readInt());
    }
}
