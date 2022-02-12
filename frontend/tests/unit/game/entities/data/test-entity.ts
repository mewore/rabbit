import { BinaryEntity } from '@/game/entities/binary-entity';
import { SignedBinaryReader } from '@/game/entities/data/signed-binary-reader';
import { SignedBinaryWriter } from '@/game/entities/data/signed-binary-writer';

export class TestEntity extends BinaryEntity {
    constructor(readonly data: number) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.data);
    }

    static decodeFromBinary(reader: SignedBinaryReader): TestEntity {
        return new TestEntity(reader.readInt());
    }
}
