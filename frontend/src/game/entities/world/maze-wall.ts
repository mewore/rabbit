import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { ConvexPolygonEntity } from '../geometry/convex-polygon-entity';

export class MazeWall extends BinaryEntity {
    constructor(
        readonly topRow: number,
        readonly leftColumn: number,
        readonly bottomRow: number,
        readonly rightColumn: number,
        readonly polygon: ConvexPolygonEntity
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.topRow);
        writer.writeInt(this.leftColumn);
        writer.writeInt(this.bottomRow);
        writer.writeInt(this.rightColumn);
        this.polygon.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): MazeWall {
        const topRow = reader.readInt();
        const leftColumn = reader.readInt();
        const bottomRow = reader.readInt();
        const rightColumn = reader.readInt();
        const polygon = ConvexPolygonEntity.decodeFromBinary(reader);
        return new MazeWall(topRow, leftColumn, bottomRow, rightColumn, polygon);
    }
}
