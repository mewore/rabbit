import {
    Box3,
    DataTexture,
    Frustum,
    InstancedMesh,
    LinearFilter,
    Material,
    Mesh,
    MeshBasicMaterial,
    Object3D,
    PlaneBufferGeometry,
    RGBFormat,
    Texture,
    UnsignedByteType,
    Vector3,
} from 'three';
import { BambooModel } from './bamboo-model';
import { ConvexPolygonEntity } from '../entities/geometry/convex-polygon-entity';
import { MazeMap } from '../entities/world/maze-map';

// There are a little more than 1 + 2^2 + 2^6 = 69 unique plant formations (based on the cell and its surrounding cells)
// The memory required to store the precalculated positions of all of the plants in all possible cells is
// `69 * PLANTS_PER_CELL * 3 * 4` bytes. With 1MB of memory, then there can be more than 1000 plants per cell.
// This, however, does not take into account how each *visible* cell requires PLANTS_PER_CELL * 16 * 4 bytes.
const MAX_PLANTS_PER_CELL = 400;
const PLANT_ATTEMPTS_PER_CELL = MAX_PLANTS_PER_CELL * 2;

const INFERTILITY_TEXTURE_WIDTH = 4;
const INFERTILITY_TEXTURE_HEIGHT = INFERTILITY_TEXTURE_WIDTH;
const fertilityMap: number[][] = [];
for (let i = 0; i < INFERTILITY_TEXTURE_HEIGHT; i++) {
    fertilityMap.push([]);
    for (let j = 0; j < INFERTILITY_TEXTURE_WIDTH; j++) {
        fertilityMap[i].push(0);
    }
}
const DIRT_PLANE_OFFSET = 0.1;

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
        dirtMaterial: Material,
        private readonly x: number,
        private readonly z: number,
        angleY: number,
        cellWidth: number,
        cellDepth: number,
        readonly row: number,
        readonly column: number
    ) {
        super();

        const dirtPlane = new Mesh(new PlaneBufferGeometry(cellWidth, cellDepth), dirtMaterial);
        dirtPlane.rotateX(-Math.PI / 2);
        dirtPlane.position.y = DIRT_PLANE_OFFSET;
        this.attach(dirtPlane);

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
        memorizedData: Map<number, [InstancedMesh[], Material]>,
        worldWidth: number,
        worldDepth: number,
        bambooModels: BambooModel[],
        dirtTexturePromise: Promise<Texture>
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
        if (cellKind === (1 << 10) - 1) {
            // Empty space surrounded by empty space - no need for a cell here
            return undefined;
        }

        let reusedData = memorizedData.get(cellKind);

        // TODO: Fix the rotation of remembered cells
        // To get this array, draw a 3x3 square with 8, 7, ..., 0 written in its cells and rotate it.
        const rotationBitIndices = [2, 5, 8, 1, 4, 7, 0, 3, 6];
        function rotateBy90DegreesClockwise(kind: number): number {
            return rotationBitIndices.reduce((previous, bitIndex) => (previous << 1) | ((kind >> bitIndex) & 1), 0);
        }
        let currentAngle = 0;
        for (let i = 0; i < 3 && !reusedData; i++) {
            currentAngle += Math.PI / 2;
            cellKind = rotateBy90DegreesClockwise(cellKind);
            reusedData = memorizedData.get(cellKind);
        }

        // TODO: Fix the bug where some cells have no plants or less plants than they should have
        let instancesToUse: InstancedMesh[];
        let dirtMaterialToUse: Material;
        const cellWidth = worldWidth / mapData.width;
        const cellDepth = worldDepth / mapData.height;
        const x = (column / mapData.width - 0.5) * worldWidth;
        const z = (row / mapData.height - 0.5) * worldDepth;
        // reusedInstances = undefined;
        if (reusedData) {
            // Attaching meshes to multiple parents doesn't work so they must be copied/
            // However, reusing matrices between instanced meshes *is* allowed.
            [instancesToUse, dirtMaterialToUse] = [ForestCell.copyInstances(reusedData[0]), reusedData[1]];
        } else {
            currentAngle = 0;
            [instancesToUse, dirtMaterialToUse] = ForestCell.generate(
                mapData,
                row,
                column,
                worldWidth,
                worldDepth,
                bambooModels,
                dirtTexturePromise
            );
            memorizedData.set(originalCellKind, [instancesToUse, dirtMaterialToUse]);
        }
        const cell = new ForestCell(
            instancesToUse,
            dirtMaterialToUse,
            x + cellWidth / 2,
            z + cellDepth / 2,
            currentAngle,
            cellWidth,
            cellDepth,
            row,
            column
        );
        cell.debugData = reusedData
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

    static generate(
        mapData: MazeMap,
        row: number,
        column: number,
        worldWidth: number,
        worldDepth: number,
        bambooModels: BambooModel[],
        dirtTexturePromise: Promise<Texture>
    ): [InstancedMesh[], Material] {
        // Normalized
        const minX = column / mapData.width;
        const rangeX = 1 / mapData.width;
        const minZ = row / mapData.height;
        const rangeZ = 1 / mapData.height;

        let plantX: number;
        let plantZ: number;
        let fertility: number;
        let height: number;
        const offsetsX = [0, column === 0 ? 1 : NaN, column === mapData.width - 1 ? -1 : NaN].filter((a) => !isNaN(a));
        const offsetsZ = [0, row === 0 ? 1 : NaN, row === mapData.height - 1 ? -1 : NaN].filter((a) => !isNaN(a));
        const distanceDivisor = MAX_DISTANCE_FROM_WALL / Math.max(mapData.width, mapData.height);
        const distanceDivisorSquared = distanceDivisor * distanceDivisor;

        const plants: number[][] = bambooModels.map(() => []);

        const relevantPolygons: ReadonlyArray<ConvexPolygonEntity> = mapData.getRelevantPolygons(row, column);

        // The inside of the loops here may become a performance bottleneck!
        const alphaData = new Uint8Array(INFERTILITY_TEXTURE_WIDTH * INFERTILITY_TEXTURE_HEIGHT * 4);

        let index = 0;
        let x = minX;
        let z = minZ;
        const xStep = rangeX / (INFERTILITY_TEXTURE_WIDTH - 1);
        const zStep = rangeZ / (INFERTILITY_TEXTURE_HEIGHT - 1);
        let infertility: number;
        for (let i = 0; i < INFERTILITY_TEXTURE_HEIGHT; i++, z += zStep) {
            x = minX;
            for (let j = 0; j < INFERTILITY_TEXTURE_WIDTH; j++, x += xStep) {
                fertility = ForestCell.getFertility(x, z, offsetsX, offsetsZ, relevantPolygons, distanceDivisorSquared);
                infertility = Math.floor((1 - fertility) * (1 - fertility) * 255.9);
                alphaData[index++] = infertility;
                alphaData[index++] = infertility;
                alphaData[index++] = infertility;
                fertilityMap[i][j] = fertility;
            }
        }

        for (let i = 0; i < PLANT_ATTEMPTS_PER_CELL && plants.length < MAX_PLANTS_PER_CELL * 2; i++) {
            plantX = minX + Math.random() * rangeX;
            plantZ = minZ + Math.random() * rangeZ;
            fertility = ForestCell.getFertility(
                plantX,
                plantZ,
                offsetsX,
                offsetsZ,
                relevantPolygons,
                distanceDivisorSquared,
                0
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

        const instances = plants.flatMap((plantPositions, index): InstancedMesh[] =>
            bambooModels[index].makeInstances(plantPositions, (index + 1) / bambooModels.length)
        );
        const dirtAlphaTexture = new DataTexture(
            alphaData,
            INFERTILITY_TEXTURE_WIDTH,
            INFERTILITY_TEXTURE_HEIGHT,
            RGBFormat,
            UnsignedByteType,
            undefined,
            undefined,
            undefined,
            LinearFilter
        );
        dirtAlphaTexture.flipY = true;
        const material = new MeshBasicMaterial({ wireframe: true, transparent: true });
        dirtTexturePromise.then((dirtTexture) => {
            material.map = dirtTexture;
            material.alphaMap = dirtAlphaTexture;
            material.wireframe = false;
            material.needsUpdate = true;
        });
        return [instances, material];
    }

    private static getFertility(
        x: number,
        y: number,
        offsetsX: number[],
        offsetsY: number[],
        relevantPolygons: ReadonlyArray<ConvexPolygonEntity>,
        distanceDivisorSquared: number,
        innerValue = 1
    ): number {
        minPlantDistanceSquared = Infinity;
        for (const offsetX of offsetsX) {
            for (const offsetY of offsetsY) {
                for (const polygon of relevantPolygons) {
                    if (polygon.containsPoint(x + offsetX, y + offsetY)) {
                        return innerValue;
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
