import { BinaryEntity } from '../binary-entity';
import { MazeMap } from '../world/maze-map';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

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
