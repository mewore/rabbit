import {
    BackSide,
    BufferAttribute,
    BufferGeometry,
    DoubleSide,
    Mesh,
    MeshStandardMaterial,
    MirroredRepeatWrapping,
    TextureLoader,
    Vector2,
    Vector3,
    sRGBEncoding,
} from 'three';
import { Body, Box, ConvexPolyhedron, Material, Vec3, World } from 'cannon-es';
import { CannonDebugRenderer } from '../util/cannon-debug-renderer';
import { MazeMap } from '../entities/world/maze-map';
import { Updatable } from '../util/updatable';

const HEIGHT = 100.0;
const PHYSICS_MATERIAL = new Material({ friction: 0, restitution: 0 });
const PADDING_COEFFICIENT = 0.1;
const DISTANCE_TO_UV_MULTIPLIER = 1 / HEIGHT;

interface WithDynamicInfo {
    active?: boolean;
    playerCell?: number;
}

export class ForestWall extends Mesh<BufferGeometry, MeshStandardMaterial> implements Updatable {
    private mapData?: MazeMap;

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

    update(): void {}

    beforeRender(): void {}

    generate(mapData: MazeMap): void {
        const positions: number[] = [];
        const uvPositions: number[] = [];

        const worldWidth = mapData.width;
        const worldDepth = mapData.depth;

        this.mapData = mapData;
        const columnCountInfo = { length: mapData.columnCount };
        this.bodiesPerCell = Array.from({ length: mapData.rowCount }, () => Array.from(columnCountInfo));

        this.totalWallCount = mapData.walls.length;
        for (const wall of mapData.walls) {
            const polygon = wall.polygon;
            const vertices: Vec3[] = [];
            for (let i = 0; i < polygon.points.length; i++) {
                const next = (i + 1) % polygon.points.length;
                const x1 = polygon.points[i].x * worldWidth - worldWidth / 2;
                const z1 = polygon.points[i].y * worldDepth - worldDepth / 2;
                const x2 = polygon.points[next].x * worldWidth - worldWidth / 2;
                const z2 = polygon.points[next].y * worldDepth - worldDepth / 2;
                const distance = Math.sqrt((x1 - x2) * (x1 - x2) + (z1 - z2) * (z1 - z2));
                const rightUvX = distance * DISTANCE_TO_UV_MULTIPLIER;

                for (const offset of mapData.wrappingOffsets) {
                    // First triangle (bottom-left, bottom-right, top-right)
                    positions.push(x1 + offset.x, 0.0, z1 + offset.z);
                    uvPositions.push(0, 1);

                    positions.push(x2 + offset.x, 0.0, z2 + offset.z);
                    uvPositions.push(rightUvX, 1);

                    positions.push(x2 + offset.x, HEIGHT, z2 + offset.z);
                    uvPositions.push(rightUvX, 0);

                    // Second triangle (bottom-left, top-right, top-left)
                    positions.push(x1 + offset.x, 0.0, z1 + offset.z);
                    uvPositions.push(0, 1);

                    positions.push(x2 + offset.x, HEIGHT, z2 + offset.z);
                    uvPositions.push(rightUvX, 0);

                    positions.push(x1 + offset.x, HEIGHT, z1 + offset.z);
                    uvPositions.push(0, 0);
                }

                vertices.push(new Vec3(x1, 0, z1), new Vec3(x1, HEIGHT, z1));
            }

            let body: Body;
            if (polygon.points.length === 4) {
                const xValues = polygon.points.map((point) => point.x * worldWidth - worldWidth / 2);
                const minX = xValues.reduce((previous, current) => Math.min(previous, current));
                const maxX = xValues.reduce((previous, current) => Math.max(previous, current));
                const zValues = polygon.points.map((point) => point.y * worldDepth - worldDepth / 2);
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
                position = position.scale(1.0 / polygon.points.length);
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

        this.geometry.setAttribute('position', new BufferAttribute(new Float32Array(positions), 3));
        this.geometry.setAttribute('uv', new BufferAttribute(new Float32Array(uvPositions), 2));
        this.geometry.computeVertexNormals();
    }
}
