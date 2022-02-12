import { BinaryEntity } from '../binary-entity';
import { ForestData } from '../world/forest-data';
import { MapData } from '../world/map-data';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class MapDataMessage extends BinaryEntity {
    constructor(readonly map: MapData, readonly forest: ForestData) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.map.appendToBinaryOutput(writer);
        this.forest.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): MapDataMessage {
        return new MapDataMessage(MapData.decodeFromBinary(reader), ForestData.decodeFromBinary(reader));
    }
}
