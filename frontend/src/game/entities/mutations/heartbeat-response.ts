import { BinaryEntity } from '../binary-entity';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { MutationType } from './mutation-type';

export class HeartbeatResponse extends BinaryEntity {
    constructor(readonly id: number) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeByte(MutationType.HEARTBEAT_RESPONSE);
        writer.writeInt(this.id);
    }
}
