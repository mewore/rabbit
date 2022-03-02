import { Body, Box, ConvexPolyhedron, Material, Vec3, World } from 'cannon-es';
import {
    BackSide,
    BufferAttribute,
    BufferGeometry,
    DoubleSide,
    Mesh,
    MeshStandardMaterial,
    MirroredRepeatWrapping,
    sRGBEncoding,
    TextureLoader,
    Vector2,
    Vector3,
} from 'three';

import { MazeMap } from '../entities/world/maze-map';
import { MazeWall } from '../entities/world/maze-wall';
import { CannonDebugRenderer } from '../util/cannon-debug-renderer';
import { LazyLoadAllocation } from '../util/lazy-load-allocation';
import { Updatable } from '../util/updatable';

const HEIGHT = 100.0;
const PHYSICS_MATERIAL = new Material({ friction: 0, restitution: 0 });
const PADDING_COEFFICIENT = 0.1;
const DISTANCE_TO_UV_MULTIPLIER = 1 / HEIGHT;

interface WithDynamicInfo {
    active?: boolean;
    playerCell?: number;
}

interface WithExtraWallInfo {
    distanceFromCenter?: number;
}

export class ForestWall extends Mesh<BufferGeometry, MeshStandardMaterial> implements Updatable {
    private mapData?: MazeMap;

    readonly wallLazyLoad = new LazyLoadAllocation();
    private wallsToLoad: (MazeWall & WithExtraWallInfo)[] = [];
    private nextWallIndex = 0;
    private posIndex = -1;
    private positions = new Float32Array();
    private uvIndex = -1;
    private uvPositions = new Float32Array();

    private activeWalls: (Body & WithDynamicInfo)[] = [];
    private totalWallCount = 0;
    private lastPlayerCell = -1;
    private bodiesPerCell?: ((Body & WithDynamicInfo)[] | undefined)[][];

    padding = 0;

    constructor(private readonly playerPosition: Vector3, private readonly physicsWorld: World) {
        super(
            new BufferGeometry(),
            new MeshStandardMaterial({
                side: DoubleSide,
                shadowSide: BackSide,
                transparent: true,
                alphaTest: 0.5,
            })
        );
        this.castShadow = true;

        const textureLoader = new TextureLoader();
        Promise.all([
            textureLoader.loadAsync('./assets/bamboo/wall.png'),
            textureLoader.loadAsync('./assets/bamboo/wall-normal.png'),
            textureLoader.loadAsync('./assets/bamboo/wall-alpha.png'),
        ]).then(([texture, normal, alpha]) => {
            texture.encoding = sRGBEncoding;
            for (const currentTexture of [texture, normal, alpha]) {
                currentTexture.wrapS = currentTexture.wrapT = MirroredRepeatWrapping;
                currentTexture.flipY = false;
            }
            this.material.map = texture;
            this.material.normalMap = normal;
            this.material.normalScale = new Vector2(25, 25);
            this.material.alphaMap = alpha;
            this.receiveShadow = true;
            this.material.needsUpdate = true;
        });
    }

    get activeBodyCount(): number {
        return this.activeWalls.length;
    }

    get totalBodyCount(): number {
        return this.totalWallCount;
    }

    beforePhysics(): void {
        if (!this.mapData || !this.bodiesPerCell) {
            return;
        }
        const column = this.mapData.getColumn(this.playerPosition.x);
        const row = this.mapData.getRow(this.playerPosition.z);
        const cell = row * this.mapData.columnCount + column;
        if (this.lastPlayerCell === cell) {
            return;
        }
        this.lastPlayerCell = cell;
        const cellPadding = Math.ceil(this.padding / this.mapData.cellSize);
        for (let i = row - cellPadding; i <= row + cellPadding; i++) {
            for (let j = column - cellPadding; j <= column + cellPadding; j++) {
                const bodies = this.bodiesPerCell[this.mapData.wrapRow(i)][this.mapData.wrapColumn(j)];
                if (bodies) {
                    for (const body of bodies) {
                        body.playerCell = cell;
                        this.mapData.wrapTowards(body.position, this.playerPosition);
                        if (!body.active) {
                            body.active = true;
                            this.physicsWorld.addBody(body);
                            this.activeWalls.push(body);
                        }
                    }
                }
            }
        }
        this.activeWalls = this.activeWalls.filter((body) => {
            if (body.playerCell === cell) {
                return true;
            }
            body.active = false;
            this.physicsWorld.removeBody(body);
            return false;
        });
    }

    afterPhysics(): void {}

    update(): void {
        while (this.nextWallIndex < this.wallsToLoad.length && this.wallLazyLoad.tryToUse(5)) {
            this.loadWalls(Math.min(10, this.wallsToLoad.length - this.nextWallIndex));
            if (this.nextWallIndex === this.wallsToLoad.length) {
                this.geometry.deleteAttribute('normal');
                this.geometry.setAttribute('position', new BufferAttribute(this.positions, 3));
                this.geometry.setAttribute('uv', new BufferAttribute(this.uvPositions, 2));
                this.geometry.computeVertexNormals();
            }
        }
    }

    beforeRender(): void {}

    generate(mapData: MazeMap, wallsToLoadImmediately: number): void {
        this.mapData = mapData;
        const columnCountInfo = { length: mapData.columnCount };
        this.bodiesPerCell = Array.from({ length: mapData.rowCount }, () => Array.from(columnCountInfo));

        const middleRow = mapData.rowCount / 2;
        const middleColumn = mapData.columnCount / 2;
        this.wallsToLoad = mapData.walls.slice();
        let totalVertexCount = 0;
        for (const wall of this.wallsToLoad) {
            wall.distanceFromCenter =
                Math.max(Math.abs(wall.topRow - middleRow), Math.abs(wall.bottomRow - middleRow)) +
                Math.max(Math.abs(wall.leftColumn - middleColumn), Math.abs(wall.rightColumn - middleColumn));
            totalVertexCount += wall.polygon.points.length * 6 * 9;
        }
        this.wallsToLoad = this.wallsToLoad.sort(
            (first, second) => (first.distanceFromCenter || 0) - (second.distanceFromCenter || 0)
        );
        this.positions = new Float32Array(totalVertexCount * 3);
        this.posIndex = -1;
        this.uvPositions = new Float32Array(totalVertexCount * 2);
        this.uvIndex = -1;

        this.totalWallCount = mapData.walls.length;
        this.nextWallIndex = 0;

        const loadedVertices = this.loadWalls(Math.min(wallsToLoadImmediately, this.wallsToLoad.length));
        const initialPosAttribute = new BufferAttribute(this.positions, 3);
        initialPosAttribute.count = loadedVertices;
        this.geometry.setAttribute('position', initialPosAttribute);
        const initialUvAttribute = new BufferAttribute(this.uvPositions, 2);
        initialUvAttribute.count = loadedVertices;
        this.geometry.setAttribute('uv', initialUvAttribute);
        this.geometry.computeVertexNormals();
    }

    private loadWalls(count: number): number {
        if (!this.mapData || !this.bodiesPerCell) {
            return 0;
        }
        let loadedVertices = 0;
        const worldWidth = this.mapData.width;
        const worldDepth = this.mapData.depth;

        for (let i = 0; i < count; i++) {
            const wall = this.wallsToLoad[this.nextWallIndex++];
            const polygon = wall.polygon;
            const vertices: Vec3[] = [];
            const points = polygon.points;
            let x1 = (points[points.length - 1].x - 0.5) * worldWidth;
            let z1 = (points[points.length - 1].y - 0.5) * worldDepth;
            const pos = this.positions;
            let posIndex = this.posIndex;
            const uv = this.uvPositions;
            let uvIndex = this.uvIndex;
            for (const point of points) {
                const x2 = (point.x - 0.5) * worldWidth;
                const z2 = (point.y - 0.5) * worldDepth;
                const distance = Math.sqrt((x1 - x2) * (x1 - x2) + (z1 - z2) * (z1 - z2));
                const rightUvX = distance * DISTANCE_TO_UV_MULTIPLIER;

                for (const offset of this.mapData.wrappingOffsets) {
                    // First triangle (bottom-left, bottom-right, top-right)
                    (pos[++posIndex] = x1 + offset.x), (pos[++posIndex] = 0.0), (pos[++posIndex] = z1 + offset.z);
                    (uv[++uvIndex] = 0), (uv[++uvIndex] = 1);

                    (pos[++posIndex] = x2 + offset.x), (pos[++posIndex] = 0.0), (pos[++posIndex] = z2 + offset.z);
                    (uv[++uvIndex] = rightUvX), (uv[++uvIndex] = 1);

                    (pos[++posIndex] = x2 + offset.x), (pos[++posIndex] = HEIGHT), (pos[++posIndex] = z2 + offset.z);
                    (uv[++uvIndex] = rightUvX), (uv[++uvIndex] = 0);
                    loadedVertices += 3;

                    // Second triangle (bottom-left, top-right, top-left)
                    (pos[++posIndex] = x1 + offset.x), (pos[++posIndex] = 0.0), (pos[++posIndex] = z1 + offset.z);
                    (uv[++uvIndex] = 0), (uv[++uvIndex] = 1);

                    (pos[++posIndex] = x2 + offset.x), (pos[++posIndex] = HEIGHT), (pos[++posIndex] = z2 + offset.z);
                    (uv[++uvIndex] = rightUvX), (uv[++uvIndex] = 0);

                    (pos[++posIndex] = x1 + offset.x), (pos[++posIndex] = HEIGHT), (pos[++posIndex] = z1 + offset.z);
                    (uv[++uvIndex] = 0), (uv[++uvIndex] = 0);
                    loadedVertices += 3;
                }

                vertices.push(new Vec3(x1, 0, z1), new Vec3(x1, HEIGHT, z1));
                x1 = x2;
                z1 = z2;
            }
            this.posIndex = posIndex;
            this.uvIndex = uvIndex;

            let body: Body;
            if (points.length === 4) {
                const xValues = points.map((point) => point.x * worldWidth - worldWidth / 2);
                const minX = xValues.reduce((previous, current) => Math.min(previous, current));
                const maxX = xValues.reduce((previous, current) => Math.max(previous, current));
                const zValues = points.map((point) => point.y * worldDepth - worldDepth / 2);
                const minZ = zValues.reduce((previous, current) => Math.min(previous, current));
                const maxZ = zValues.reduce((previous, current) => Math.max(previous, current));
                body = new Body({
                    shape: new Box(new Vec3((maxX - minX) / 2, HEIGHT / 2, (maxZ - minZ) / 2)),
                    position: new Vec3((maxX + minX) / 2, HEIGHT / 2, (maxZ + minZ) / 2),
                    material: PHYSICS_MATERIAL,
                    allowSleep: true,
                    fixedRotation: true,
                });
            } else {
                let position = new Vec3();
                for (let i = 0; i < vertices.length; i += 2) {
                    position = position.vadd(vertices[i]);
                }
                position = position.scale(1.0 / points.length);
                if (
                    position.x < -worldWidth / 2 - PADDING_COEFFICIENT * worldWidth ||
                    position.x > worldWidth / 2 + PADDING_COEFFICIENT * worldWidth ||
                    position.z < -worldDepth / 2 - PADDING_COEFFICIENT * worldDepth ||
                    position.z > worldDepth / 2 + PADDING_COEFFICIENT * worldDepth
                ) {
                    continue;
                }

                for (let i = 0; i < vertices.length; i++) {
                    vertices[i] = vertices[i].vsub(position);
                }
                const faces: number[][] = [];
                for (let i = 0; i < vertices.length; i += 2) {
                    faces.push(
                        [i, (i + 2) % vertices.length, (i + 3) % vertices.length],
                        [i, (i + 3) % vertices.length, i + 1]
                    );
                }
                const shape = new ConvexPolyhedron({ vertices, faces });
                CannonDebugRenderer.setShapeColor(shape, 0xff0000);
                body = new Body({
                    shape,
                    position,
                    material: PHYSICS_MATERIAL,
                    allowSleep: true,
                    fixedRotation: true,
                });
            }
            for (let i = wall.topRow; i <= wall.bottomRow; i++) {
                for (let j = wall.leftColumn; j <= wall.rightColumn; j++) {
                    (this.bodiesPerCell[i][j] = this.bodiesPerCell[i][j] || []).push(body);
                }
            }
        }
        return loadedVertices;
    }
}
