import { BinaryEntity } from '../binary-entity';
import { ConvexPolygonEntity } from '../geometry/convex-polygon-entity';
import { MazeWall } from './maze-wall';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

const wallReach = 1;

export class MazeMap extends BinaryEntity {
    private readonly relevantPolygons: ReadonlyArray<ConvexPolygonEntity>[][];

    constructor(
        readonly width: number,
        readonly height: number,
        private readonly map: ReadonlyArray<ReadonlyArray<boolean>>,
        readonly walls: ReadonlyArray<MazeWall>
    ) {
        super();
        const relevantPolygons: ConvexPolygonEntity[][][] = [];
        for (let i = 0; i < height; i++) {
            relevantPolygons.push([]);
            for (let j = 0; j < width; j++) {
                relevantPolygons[i].push([]);
            }
        }
        for (const wall of walls) {
            for (let i = wall.topRow - wallReach; i <= wall.bottomRow + wallReach; i++) {
                for (let j = wall.leftColumn - wallReach; j <= wall.rightColumn + wallReach; j++) {
                    relevantPolygons[this.wrapRow(i)][this.wrapColumn(j)].push(wall.polygon);
                }
            }
        }
        this.relevantPolygons = relevantPolygons;
    }

    getCell(row: number, column: number): boolean {
        return this.map[this.wrapRow(row)][this.wrapColumn(column)];
    }

    getRelevantPolygons(row: number, column: number): ReadonlyArray<ConvexPolygonEntity> {
        return this.relevantPolygons[this.wrapRow(row)][this.wrapColumn(column)];
    }

    wrapRow(row: number): number {
        return (row + this.height) % this.height;
    }

    wrapColumn(column: number): number {
        return (column + this.width) % this.width;
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.height);
        writer.writeInt(this.width);
        for (let i = 0; i < this.height; i++) {
            for (let j = 0; j < this.width; j++) {
                writer.writeBoolean(this.map[i][j]);
            }
        }
        writer.writeEntityArray(this.walls);
    }

    static decodeFromBinary(reader: SignedBinaryReader): MazeMap {
        const height = reader.readInt();
        const width = reader.readInt();
        const map: boolean[][] = [];
        for (let i = 0; i < height; i++) {
            map.push([]);
            for (let j = 0; j < width; j++) {
                map[i].push(reader.readBoolean());
            }
        }
        return new MazeMap(width, height, map, reader.readEntityArray(MazeWall));
    }
}
