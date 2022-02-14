import { Box3, Frustum, InstancedMesh, Object3D, Vector3 } from 'three';
import { BambooModel } from './bamboo-model';
import { ConvexPolygonEntity } from '../entities/geometry/convex-polygon-entity';
import { MazeMap } from '../entities/world/maze-map';

// There are a little more than 1 + 2^2 + 2^6 = 69 unique plant formations (based on the cell and its surrounding cells)
// The memory required to store the precalculated positions of all of the plants in all possible cells is
// `69 * PLANTS_PER_CELL * 3 * 4` bytes. With 1MB of memory, then there can be more than 1000 plants per cell.
// This, however, does not take into account how each *visible* cell requires PLANTS_PER_CELL * 16 * 4 bytes.
const MAX_PLANTS_PER_CELL = 400;
const PLANT_ATTEMPTS_PER_CELL = MAX_PLANTS_PER_CELL * 2;

const PLANT_PADDING = 15;
const PLANT_HEIGHT = 100;

const MAX_DISTANCE_FROM_WALL = 0.7;

const HEIGHT_VARIATION = 0.4;
const ZERO_HEIGHT_CHANCE = 0.2;

// Discovered through trial and error
// const FRUSTUM_FAR_PLANE_INDEX = 4;
const FRUSTUM_NEAR_PLANE_INDEX = 5;

let minPlantDistanceSquared: number;
const tmpVector3 = new Vector3();

export class ForestCell extends Object3D {
    debugData = '';
    private readonly boundingBox: Box3;
    readonly count: number;

    static spawnAttempts = 0;

    constructor(
        private readonly instancedMeshes: InstancedMesh[],
        private readonly x: number,
        private readonly z: number,
        angleY: number,
        cellWidth: number,
        cellDepth: number,
        readonly row: number,
        readonly column: number
    ) {
        super();
        for (const mesh of instancedMeshes) {
            this.attach(mesh);
            mesh.position.set(-cellWidth / 2, 0, -cellDepth / 2);
        }
        this.boundingBox = new Box3(
            new Vector3(x - cellWidth / 2 - PLANT_PADDING, 0, z - cellDepth / 2 - PLANT_PADDING),
            new Vector3(x + cellWidth / 2 + PLANT_PADDING, PLANT_HEIGHT, z + cellDepth / 2 + PLANT_PADDING)
        );

        let count = 0;
        for (let i = 0; i < instancedMeshes.length; i += 2) {
            count += instancedMeshes[i].count;
        }
        this.count = count;

        this.rotateY(angleY);
        this.position.set(x, 0, z);

        this.visible = false;
    }

    setReceiveShadow(receiveShadow: boolean): void {
        for (const mesh of this.instancedMeshes) {
            mesh.receiveShadow = receiveShadow;
        }
    }

    reposition(offsetX: number, offsetZ: number): void {
        tmpVector3.set(this.x + offsetX, 0, this.z + offsetZ).sub(this.position);
        this.position.add(tmpVector3);
        this.boundingBox.translate(tmpVector3);
    }

    static fromMapData(
        mapData: MazeMap,
        row: number,
        column: number,
        memorizedInstances: Map<number, InstancedMesh[]>,
        worldWidth: number,
        worldDepth: number,
        bambooModels: BambooModel[]
    ): ForestCell | undefined {
        if (!mapData.getCell(row, column)) {
            let neighbouringWalls = 0;
            const dy = [-1, 1, 0, 0];
            const dx = [0, 0, -1, 1];
            for (let i = 0; i < 4; i++) {
                neighbouringWalls += mapData.getCell(row + dy[i], column + dx[i]) ? 0 : 1;
            }
            if (neighbouringWalls >= 3) {
                return undefined;
            }
        }

        let cellKind = 0;
        for (let rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (let colOffset = -1; colOffset <= 1; colOffset++) {
                cellKind = (cellKind << 1) | (mapData.getCell(row + rowOffset, column + colOffset) ? 1 : 0);
            }
        }
        const originalCellKind = cellKind;
        let reusedInstances = memorizedInstances.get(cellKind);

        // TODO: Fix the rotation of remembered cells
        // To get this array, draw a 3x3 square with 8, 7, ..., 0 written in its cells and rotate it.
        const rotationBitIndices = [2, 5, 8, 1, 4, 7, 0, 3, 6];
        function rotateBy90DegreesClockwise(kind: number): number {
            return rotationBitIndices.reduce((previous, bitIndex) => (previous << 1) | ((kind >> bitIndex) & 1), 0);
        }
        let currentAngle = 0;
        for (let i = 0; i < 3 && !reusedInstances; i++) {
            currentAngle += Math.PI / 2;
            cellKind = rotateBy90DegreesClockwise(cellKind);
            reusedInstances = memorizedInstances.get(cellKind);
        }

        // TODO: Fix the bug where some cells have no plants or less plants than they should have
        let instancesToUse: InstancedMesh[];
        const cellWidth = worldWidth / mapData.width;
        const cellDepth = worldDepth / mapData.height;
        const x = (column / mapData.width - 0.5) * worldWidth;
        const z = (row / mapData.height - 0.5) * worldDepth;
        // reusedInstances = undefined;
        if (reusedInstances) {
            // Attaching meshes to multiple parents doesn't work so they must be copied/
            // However, reusing matrices between instanced meshes *is* allowed.
            instancesToUse = ForestCell.copyInstances(reusedInstances);
        } else {
            currentAngle = 0;
            instancesToUse = ForestCell.generateInstances(mapData, row, column, worldWidth, worldDepth, bambooModels);
            memorizedInstances.set(originalCellKind, instancesToUse);
        }
        const cell = new ForestCell(
            instancesToUse,
            x + cellWidth / 2,
            z + cellDepth / 2,
            currentAngle,
            cellWidth,
            cellDepth,
            row,
            column
        );
        cell.debugData = reusedInstances
            ? `Reused(${cellKind} -(${currentAngle / (Math.PI / 2)})-> ${originalCellKind})`
            : 'Original';
        return cell;
    }

    private static copyInstances(instancedMeshes: InstancedMesh[]): InstancedMesh[] {
        return instancedMeshes.map((instancedMesh) => {
            const result = new InstancedMesh(instancedMesh.geometry, instancedMesh.material, instancedMesh.count);
            result.instanceMatrix = instancedMesh.instanceMatrix;
            result.count = instancedMesh.count;
            return result;
        });
    }

    static generateInstances(
        mapData: MazeMap,
        row: number,
        column: number,
        worldWidth: number,
        worldDepth: number,
        bambooModels: BambooModel[]
    ): InstancedMesh[] {
        // Normalized
        const minX = column / mapData.width;
        const rangeX = 1 / mapData.width;
        const minZ = row / mapData.height;
        const rangeZ = 1 / mapData.height;

        let plantX: number;
        let plantZ: number;
        let index: number;
        let fertility: number;
        let height: number;
        const offsetsX = [0, column === 0 ? 1 : NaN, column === mapData.width - 1 ? -1 : NaN].filter((a) => !isNaN(a));
        const offsetsZ = [0, row === 0 ? 1 : NaN, row === mapData.height - 1 ? -1 : NaN].filter((a) => !isNaN(a));
        const distanceDivisor = MAX_DISTANCE_FROM_WALL / Math.max(mapData.width, mapData.height);
        const distanceDivisorSquared = distanceDivisor * distanceDivisor;

        const plants: number[][] = bambooModels.map(() => []);

        const relevantPolygons: ReadonlyArray<ConvexPolygonEntity> = mapData.getRelevantPolygons(row, column);
        // The inside of this loop may become a performance bottleneck!
        for (let i = 0; i < PLANT_ATTEMPTS_PER_CELL && plants.length < MAX_PLANTS_PER_CELL * 2; i++) {
            plantX = minX + Math.random() * rangeX;
            plantZ = minZ + Math.random() * rangeZ;
            fertility = ForestCell.getFertility(
                plantX,
                plantZ,
                offsetsX,
                offsetsZ,
                relevantPolygons,
                distanceDivisorSquared
            );
            ForestCell.spawnAttempts++;
            fertility *= fertility;
            fertility *= fertility;
            if (fertility < 0.0000001 || Math.random() > fertility) {
                continue;
            }
            if (Math.random() < ZERO_HEIGHT_CHANCE) {
                height = 0;
            } else {
                height = Math.min(Math.max(Math.random() * fertility + (Math.random() - 0.5) * HEIGHT_VARIATION, 0), 1);
            }
            index = Math.min(Math.floor(height * bambooModels.length), bambooModels.length - 1);
            plants[index].push((plantX - minX) * worldWidth, (plantZ - minZ) * worldDepth);
        }

        return plants.flatMap((plantPositions, index): InstancedMesh[] =>
            bambooModels[index].makeInstances(plantPositions, (index + 1) / bambooModels.length)
        );
    }

    private static getFertility(
        x: number,
        y: number,
        offsetsX: number[],
        offsetsY: number[],
        relevantPolygons: ReadonlyArray<ConvexPolygonEntity>,
        distanceDivisorSquared: number
    ): number {
        minPlantDistanceSquared = Infinity;
        for (const offsetX of offsetsX) {
            for (const offsetY of offsetsY) {
                for (const polygon of relevantPolygons) {
                    if (polygon.containsPoint(x + offsetX, y + offsetY)) {
                        return 0;
                    }
                    minPlantDistanceSquared = Math.min(
                        minPlantDistanceSquared,
                        polygon.distanceToPointSquared(x + offsetX, y + offsetY)
                    );
                }
            }
        }
        return minPlantDistanceSquared < distanceDivisorSquared
            ? 1.0 - Math.sqrt(minPlantDistanceSquared / distanceDivisorSquared)
            : 0.0;
    }

    /**
     * Cull the instances of this instanced mesh - skip rendering the instances which don't overlap the frustum.
     *
     * @param frustum The frustum to check against.
     * @param farPlaneTransition The distance from the far plane of the frustum at which the instances start to get
     * squashed until they become zero-width as a makeshift fade-out effect.
     * @param nearPlaneTransition The distance from the far plane of the frustum at which the instances finish getting
     * expanded until they become full-width as a makeshift fade-in effect. If unset, then there is no fade-in effect.
     * @returns The number of instances that do overlap with the frustum.
     */
    cull(frustum: Frustum, fadeOutPortion: number, viewDistance: number, visibilityCoefficient: number): number {
        this.visible = frustum.intersectsBox(this.boundingBox);
        if (!this.visible) {
            return 0;
        }

        let closeness = 1 - frustum.planes[FRUSTUM_NEAR_PLANE_INDEX].distanceToPoint(this.position) / viewDistance;
        closeness *= closeness;
        closeness *= closeness;
        const visibility = Math.min(closeness / fadeOutPortion, 1) * visibilityCoefficient;
        if (visibility < 0.001) {
            this.visible = false;
            return 0;
        }

        let visible = 0;

        for (const mesh of this.instancedMeshes) {
            mesh.count = Math.floor(visibility * mesh.instanceMatrix.count);
            visible += mesh.count;
        }

        return visible;
    }
}
