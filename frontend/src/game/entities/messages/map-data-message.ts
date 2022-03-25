import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { DummyBox } from '../world/dummy-box';
import { MazeMap } from '../world/maze-map';

export class MapDataMessage extends BinaryEntity {
    constructor(readonly map: MazeMap, readonly dummyBoxes: DummyBox[]) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.map.appendToBinaryOutput(writer);
        writer.writeEntityArray(this.dummyBoxes);
    }

    static decodeFromBinary(reader: SignedBinaryReader): MapDataMessage {
        return new MapDataMessage(MazeMap.decodeFromBinary(reader), reader.readEntityArray(DummyBox));
    }
}
