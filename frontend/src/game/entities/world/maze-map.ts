import { Vector3 } from 'three';

import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { ConvexPolygonEntity } from '../geometry/convex-polygon-entity';
import { MazeWall } from './maze-wall';

const CELL_SIZE = 80;
const wallReach = 1;

const WRAP_X_OFFSETS = [-1, 0, 1];
const WRAP_Z_OFFSETS = [-1, 0, 1];

export class MazeMap extends BinaryEntity {
    private readonly relevantPolygons: ReadonlyArray<ConvexPolygonEntity>[][];

    readonly cellSize = CELL_SIZE;
    readonly width: number;
    readonly depth: number;
    readonly wrappingOffsets: Vector3[];

    constructor(
        readonly columnCount: number,
        readonly rowCount: number,
        private readonly map: ReadonlyArray<ReadonlyArray<boolean>>,
        readonly walls: ReadonlyArray<MazeWall>
    ) {
        super();
        this.width = columnCount * CELL_SIZE;
        this.depth = rowCount * CELL_SIZE;

        const relevantPolygons: ConvexPolygonEntity[][][] = [];
        for (let i = 0; i < rowCount; i++) {
            relevantPolygons.push([]);
            for (let j = 0; j < columnCount; j++) {
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

        this.wrappingOffsets = WRAP_X_OFFSETS.flatMap((xOffset) =>
            WRAP_Z_OFFSETS.map((zOffset) => new Vector3(xOffset * this.width, 0, zOffset * this.depth))
        );
    }

    getCell(row: number, column: number): boolean {
        return this.map[this.wrapRow(row)][this.wrapColumn(column)];
    }

    getRelevantPolygons(row: number, column: number): ReadonlyArray<ConvexPolygonEntity> {
        return this.relevantPolygons[this.wrapRow(row)][this.wrapColumn(column)];
    }

    getRow(z: number): number {
        return Math.floor((this.wrapZ(z) / this.depth + 0.5) * this.rowCount);
    }

    getColumn(x: number): number {
        return Math.floor((this.wrapX(x) / this.width + 0.5) * this.columnCount);
    }

    wrapRow(row: number): number {
        return row - Math.floor(row / this.rowCount) * this.rowCount;
    }

    wrapColumn(column: number): number {
        return column - Math.floor(column / this.columnCount) * this.columnCount;
    }

    wrapX(x: number): number {
        return x - Math.floor(x / this.width + 0.5) * this.width;
    }

    wrapZ(z: number): number {
        return z - Math.floor(z / this.depth + 0.5) * this.depth;
    }

    wrapTowards(source: { x: number; z: number }, target: { x: number; z: number }): void {
        source.x -= Math.floor((source.x - target.x) / this.width + 0.5) * this.width;
        source.z -= Math.floor((source.z - target.z) / this.depth + 0.5) * this.depth;
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.rowCount);
        writer.writeInt(this.columnCount);
        for (let i = 0; i < this.rowCount; i++) {
            for (let j = 0; j < this.columnCount; j++) {
                writer.writeBoolean(this.map[i][j]);
            }
        }
        writer.writeEntityArray(this.walls);
    }

    static decodeFromBinary(reader: SignedBinaryReader): MazeMap {
        const height = reader.readInt();
        const width = reader.readInt();
        const map: boolean[][] = Array.from({ length: height }, () => []);
        for (let i = 0; i < height; i++) {
            for (let j = 0; j < width; j++) {
                map[i].push(reader.readBoolean());
            }
        }
        return new MazeMap(width, height, map, reader.readEntityArray(MazeWall));
    }
}
