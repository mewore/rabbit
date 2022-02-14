import { BackSide, BufferAttribute, BufferGeometry, DoubleSide, Mesh, MeshBasicMaterial, Vector3 } from 'three';
import { Body, Box, ConvexPolyhedron, Material, Vec3 } from 'cannon-es';
import { CannonDebugRenderer } from '../util/cannon-debug-renderer';
import { MazeMap } from '../entities/world/maze-map';

const HEIGHT = 100.0;
const PHYSICS_MATERIAL = new Material({ friction: 0, restitution: 0 });
const PADDING_COEFFICIENT = 0.1;

export class ForestWall extends Mesh {
    readonly bodies: Body[] = [];

    constructor() {
        super(new BufferGeometry(), new MeshBasicMaterial({ side: DoubleSide, shadowSide: BackSide, color: 0 }));
        this.castShadow = true;
    }

    generate(worldWidth: number, worldDepth: number, mapData: MazeMap, offsets: Vector3[]): void {
        const positions: number[] = [];
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

                    positions.push(x1, 0.0, z1);
                    positions.push(x2, 0.0, z2);
                    positions.push(x2, HEIGHT, z2);

                    positions.push(x1, 0.0, z1);
                    positions.push(x2, HEIGHT, z2);
                    positions.push(x1, HEIGHT, z1);

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
        // this.visible = false;
    }
}
