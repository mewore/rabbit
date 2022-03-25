import Ammo from 'ammo.js';
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
import { BulletCollisionFlags } from '../physics/bullet-collision-flags';
import { LazyLoadAllocation } from '../util/lazy-load-allocation';
import { PhysicsAware } from '../util/physics-aware';
import { RenderAware } from '../util/render-aware';

const HEIGHT = 100.0;
const PHYSICS_HEIGHT = HEIGHT * 5;
const HALF_PHYSICS_HEIGHT = PHYSICS_HEIGHT / 2;
// const PADDING_COEFFICIENT = 0.1;
const DISTANCE_TO_UV_MULTIPLIER = 1 / HEIGHT;

const tmpVector3 = new Vector3();

interface WithDynamicInfo {
    active?: boolean;
    playerCell?: number;
}

interface WithExtraWallInfo {
    distanceFromCenter?: number;
}

export class ForestWall extends Mesh<BufferGeometry, MeshStandardMaterial> implements PhysicsAware, RenderAware {
    private mapData?: MazeMap;

    readonly wallLazyLoad = new LazyLoadAllocation();
    private wallsToLoad: (MazeWall & WithExtraWallInfo)[] = [];
    private nextWallIndex = 0;
    private posIndex = -1;
    private positions = new Float32Array();
    private uvIndex = -1;
    private uvPositions = new Float32Array();

    private activeWalls: (Ammo.btRigidBody & WithDynamicInfo)[] = [];
    private totalWallCount = 0;
    private lastPlayerCell = -1;
    private bodiesPerCell?: ((Ammo.btRigidBody & WithDynamicInfo)[] | undefined)[][];

    padding = 0;

    constructor(private readonly playerPosition: Vector3, private readonly physicsWorld: Ammo.btDiscreteDynamicsWorld) {
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

                        if (!body.active) {
                            body.active = true;

                            this.physicsWorld.addRigidBody(body);
                            this.activeWalls.push(body);

                            const transform = body.getWorldTransform();
                            const original = transform.getOrigin();
                            tmpVector3.set(original.x(), original.y(), original.z());
                            this.mapData.wrapTowards(tmpVector3, this.playerPosition);
                            transform.setOrigin(new Ammo.btVector3(tmpVector3.x, tmpVector3.y, tmpVector3.z));
                            body.setWorldTransform(transform);
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
            this.physicsWorld.removeRigidBody(body);
            return false;
        });
    }

    afterPhysics(): void {}

    longBeforeRender(): void {}

    beforeRender(): void {
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
        const halfWorldWidth = worldWidth * 0.5;
        const worldDepth = this.mapData.depth;
        const halfWorldDepth = worldDepth * 0.5;
        const tmpVector3 = new Ammo.btVector3();

        for (let i = 0; i < count; i++) {
            const wall = this.wallsToLoad[this.nextWallIndex++];
            const polygon = wall.polygon;
            // const vertices: Vec3[] = [];
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

                // vertices.push(new Vec3(x1, 0, z1), new Vec3(x1, HEIGHT, z1));
                x1 = x2;
                z1 = z2;
            }
            this.posIndex = posIndex;
            this.uvIndex = uvIndex;

            let body: Ammo.btRigidBody;
            if (points.length === 4) {
                // Rectangle -> box
                const minX = points.reduce((prev, current) => Math.min(prev, current.x), Infinity) * worldWidth;
                const minY = points.reduce((prev, current) => Math.min(prev, current.y), Infinity) * worldDepth;
                const maxX = points.reduce((prev, current) => Math.max(prev, current.x), -Infinity) * worldWidth;
                const maxY = points.reduce((prev, current) => Math.max(prev, current.y), -Infinity) * worldDepth;
                tmpVector3.setValue((maxX - minX) * 0.5, HALF_PHYSICS_HEIGHT, (maxY - minY) * 0.5);
                const shape = new Ammo.btBoxShape(tmpVector3);
                body = new Ammo.btRigidBody(
                    new Ammo.btRigidBodyConstructionInfo(0, new Ammo.btDefaultMotionState(), shape)
                );
                tmpVector3.setValue(
                    (maxX + minX) * 0.5 - halfWorldWidth,
                    HALF_PHYSICS_HEIGHT,
                    (maxY + minY) * 0.5 - halfWorldDepth
                );
                body.getWorldTransform().setOrigin(tmpVector3);
            } else {
                // Polygon -> polyhedron

                // The average position of the points will be the center
                const xSum = points.reduce((prev, current) => prev + current.x, 0);
                const ySum = points.reduce((prev, current) => prev + current.y, 0);
                tmpVector3.setValue(
                    (xSum * worldWidth) / points.length,
                    HALF_PHYSICS_HEIGHT,
                    (ySum * worldDepth) / points.length
                );

                const shape = new Ammo.btConvexHullShape();
                for (const point of points) {
                    const x = point.x * worldWidth - tmpVector3.x();
                    const z = point.y * worldDepth - tmpVector3.z();
                    shape.addPoint(new Ammo.btVector3(x, -HALF_PHYSICS_HEIGHT, z));
                    shape.addPoint(new Ammo.btVector3(x, HALF_PHYSICS_HEIGHT, z));
                }
                shape.recalcLocalAabb();
                body = new Ammo.btRigidBody(
                    new Ammo.btRigidBodyConstructionInfo(0, new Ammo.btDefaultMotionState(), shape)
                );

                tmpVector3.setX(tmpVector3.x() - halfWorldWidth);
                tmpVector3.setZ(tmpVector3.z() - halfWorldWidth);
                body.getWorldTransform().setOrigin(tmpVector3);
            }
            body.setCollisionFlags(BulletCollisionFlags.STATIC_OBJECT);
            for (let i = wall.topRow; i <= wall.bottomRow; i++) {
                for (let j = wall.leftColumn; j <= wall.rightColumn; j++) {
                    (this.bodiesPerCell[i][j] = this.bodiesPerCell[i][j] || []).push(body);
                }
            }
        }
        return loadedVertices;
    }
}
