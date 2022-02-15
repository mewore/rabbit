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
import { Body, Box, ConvexPolyhedron, Material, Vec3 } from 'cannon-es';
import { CannonDebugRenderer } from '../util/cannon-debug-renderer';
import { MazeMap } from '../entities/world/maze-map';

const HEIGHT = 100.0;
const PHYSICS_MATERIAL = new Material({ friction: 0, restitution: 0 });
const PADDING_COEFFICIENT = 0.1;
const DISTANCE_TO_UV_MULTIPLIER = 1 / HEIGHT;

export class ForestWall extends Mesh<BufferGeometry, MeshStandardMaterial> {
    readonly bodies: Body[] = [];

    constructor() {
        super(
            new BufferGeometry(),
            new MeshStandardMaterial({
                side: DoubleSide,
                shadowSide: BackSide,
                transparent: true,
            })
        );
        this.castShadow = true;

        const textureLoader = new TextureLoader();
        Promise.all([
            textureLoader.loadAsync('./assets/bamboo/wall.png'),
            textureLoader.loadAsync('./assets/bamboo/wall-normal.png'),
        ]).then(([texture, normal]) => {
            texture.wrapS = texture.wrapT = MirroredRepeatWrapping;
            texture.encoding = sRGBEncoding;
            texture.flipY = false;
            normal.wrapS = normal.wrapT = MirroredRepeatWrapping;
            normal.flipY = false;
            this.material.map = texture;
            this.material.normalMap = normal;
            this.material.normalScale = new Vector2(25, 25);
            this.receiveShadow = true;
            this.material.needsUpdate = true;
        });
    }

    generate(worldWidth: number, worldDepth: number, mapData: MazeMap, offsets: Vector3[]): void {
        const positions: number[] = [];
        const uvPositions: number[] = [];
        for (const offset of offsets) {
            for (const wall of mapData.walls) {
                const polygon = wall.polygon;
                const vertices: Vec3[] = [];
                for (let i = 0; i < polygon.points.length; i++) {
                    const next = (i + 1) % polygon.points.length;
                    const x1 = polygon.points[i].x * worldWidth - worldWidth / 2 + offset.x;
                    const z1 = polygon.points[i].y * worldDepth - worldDepth / 2 + offset.z;
                    const x2 = polygon.points[next].x * worldWidth - worldWidth / 2 + offset.x;
                    const z2 = polygon.points[next].y * worldDepth - worldDepth / 2 + offset.z;
                    const distance = Math.sqrt((x1 - x2) * (x1 - x2) + (z1 - z2) * (z1 - z2));
                    const rightUvX = distance * DISTANCE_TO_UV_MULTIPLIER;

                    // First triangle (bottom-left, bottom-right, top-right)
                    positions.push(x1, 0.0, z1);
                    uvPositions.push(0, 1);

                    positions.push(x2, 0.0, z2);
                    uvPositions.push(rightUvX, 1);

                    positions.push(x2, HEIGHT, z2);
                    uvPositions.push(rightUvX, 0);

                    // Second triangle (bottom-left, top-right, top-left)
                    positions.push(x1, 0.0, z1);
                    uvPositions.push(0, 1);

                    positions.push(x2, HEIGHT, z2);
                    uvPositions.push(rightUvX, 0);

                    positions.push(x1, HEIGHT, z1);
                    uvPositions.push(0, 0);

                    vertices.push(new Vec3(x1, 0, z1), new Vec3(x1, HEIGHT, z1));
                }
                if (polygon.points.length === 4) {
                    const xValues = polygon.points.map((point) => point.x * worldWidth - worldWidth / 2 + offset.x);
                    const minX = xValues.reduce((previous, current) => Math.min(previous, current));
                    const maxX = xValues.reduce((previous, current) => Math.max(previous, current));
                    const zValues = polygon.points.map((point) => point.y * worldDepth - worldDepth / 2 + offset.z);
                    const minZ = zValues.reduce((previous, current) => Math.min(previous, current));
                    const maxZ = zValues.reduce((previous, current) => Math.max(previous, current));
                    const body = new Body({
                        shape: new Box(new Vec3((maxX - minX) / 2, HEIGHT / 2, (maxZ - minZ) / 2)),
                        position: new Vec3((maxX + minX) / 2, HEIGHT / 2, (maxZ + minZ) / 2),
                        material: PHYSICS_MATERIAL,
                        allowSleep: true,
                        fixedRotation: true,
                    });
                    this.bodies.push(body);
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
                    this.bodies.push(
                        new Body({ shape, position, material: PHYSICS_MATERIAL, allowSleep: true, fixedRotation: true })
                    );
                }
            }
        }
        for (const body of this.bodies) {
            body.updateAABB();
            body.updateBoundingRadius();
            body.allowSleep = true;
            body.sleep();
        }
        this.geometry.setAttribute('position', new BufferAttribute(new Float32Array(positions), 3));
        this.geometry.setAttribute('uv', new BufferAttribute(new Float32Array(uvPositions), 2));
        this.geometry.computeVertexNormals();
    }
}
