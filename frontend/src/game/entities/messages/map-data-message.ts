import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { MazeMap } from '../world/maze-map';

export class MapDataMessage extends BinaryEntity {
    constructor(readonly map: MazeMap) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.map.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): MapDataMessage {
        return new MapDataMessage(MazeMap.decodeFromBinary(reader));
    }
}
