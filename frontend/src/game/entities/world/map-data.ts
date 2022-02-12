import { BinaryEntity } from '../binary-entity';
import { ConvexPolygonEntity } from '../geometry/convex-polygon-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class MapData extends BinaryEntity {
    constructor(
        readonly map: ReadonlyArray<ReadonlyArray<boolean>>,
        readonly polygons: ReadonlyArray<ConvexPolygonEntity>
    ) {
        super();
    }

    get height(): number {
        return this.map.length;
    }

    get width(): number {
        return this.map[0].length;
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.height);
        writer.writeInt(this.width);
        for (let i = 0; i < this.height; i++) {
            for (let j = 0; j < this.width; j++) {
                writer.writeBoolean(this.map[i][j]);
            }
        }
        writer.writeEntityArray(this.polygons);
    }

    static decodeFromBinary(reader: SignedBinaryReader): MapData {
        const height = reader.readInt();
        const width = reader.readInt();
        const map: boolean[][] = [];
        for (let i = 0; i < height; i++) {
            map.push([]);
            for (let j = 0; j < width; j++) {
                map[i].push(reader.readBoolean());
            }
        }
        return new MapData(map, reader.readEntityArray(ConvexPolygonEntity));
    }
}
