import { BinaryEntity } from '../binary-entity';
import { ForestData } from '../world/forest-data';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class ForestDataMessage extends BinaryEntity {
    constructor(readonly forest: ForestData) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.forest.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): ForestDataMessage {
        return new ForestDataMessage(ForestData.decodeFromBinary(reader));
    }
}
