import { Box, ConvexPolyhedron, Heightfield, Plane, Quaternion, Shape, Sphere, Trimesh, Vec3, World } from 'cannon-es';
import {
    BoxGeometry,
    BufferAttribute,
    BufferGeometry,
    Color,
    Mesh,
    MeshBasicMaterial,
    PlaneGeometry,
    Scene,
    SphereGeometry,
} from 'three';
import { Updatable } from './updatable';

interface ShapeDebugData {
    debugColor?: number;
    geometryId?: number;
}

/**
 * Creates THREE.js wireframe previews of all Cannon ES bodies.
 *
 * Original at:
 *
 * @class CannonDebugRenderer
 * @param {Scene} scene
 * @param {World} world
 * @param {object} [options]
 */
export class CannonDebugRenderer implements Updatable {
    readonly id = 'CannonDebugRenderer';

    active = false;

    private readonly meshes: (Mesh | undefined)[] = [];
    private readonly defaultMaterial = new MeshBasicMaterial({
        color: new Color(0.5, 255, 0.5),
        wireframe: true,
        fog: false,
    });
    private readonly sphereGeometry = new SphereGeometry(1);
    private readonly boxGeometry = new BoxGeometry(1, 1, 1);
    private readonly planeGeometry = new PlaneGeometry(10, 10, 10, 10);

    private static readonly materialByColor: Map<number, MeshBasicMaterial> = new Map();

    constructor(private readonly scene: Scene, private readonly world: World) {}

    beforePhysics(): void {}
    afterPhysics(): void {}
    beforeRender(): void {
        const bodies = this.world.bodies;
        const meshes = this.meshes;
        const shapeWorldPosition = this.tmpVec0;
        const shapeWorldQuaternion = this.tmpQuat0;

        let meshIndex = 0;

        if (this.active) {
            for (const body of bodies) {
                for (let j = 0; j !== body.shapes.length; j++) {
                    const shape = body.shapes[j];

                    this.updateMesh(meshIndex, shape);

                    const mesh = meshes[meshIndex];

                    if (mesh) {
                        // Get world position
                        body.quaternion.vmult(body.shapeOffsets[j], shapeWorldPosition);
                        body.position.vadd(shapeWorldPosition, shapeWorldPosition);

                        // Get world quaternion
                        body.quaternion.mult(body.shapeOrientations[j], shapeWorldQuaternion);

                        // Copy to meshes
                        mesh.position.set(shapeWorldPosition.x, shapeWorldPosition.y, shapeWorldPosition.z);
                        mesh.quaternion.set(
                            shapeWorldQuaternion.x,
                            shapeWorldQuaternion.y,
                            shapeWorldQuaternion.z,
                            shapeWorldQuaternion.w
                        );
                    }

                    meshIndex++;
                }
            }
        }

        for (let i = meshIndex; i < meshes.length; i++) {
            const mesh = meshes[i];
            if (mesh) {
                this.scene.remove(mesh);
            }
        }

        meshes.length = meshIndex;
    }

    tmpVec0 = new Vec3();
    tmpVec1 = new Vec3();
    tmpVec2 = new Vec3();
    tmpQuat0 = new Quaternion();

    update(): void {}

    updateMesh(index: number, shape: Shape): void {
        let mesh = this.meshes[index];
        if (!this.typeMatch(mesh, shape)) {
            if (mesh) {
                this.scene.remove(mesh);
            }
            mesh = this.meshes[index] = this.createMesh(shape);
        }
        if (mesh) {
            this.scaleMesh(mesh, shape);
        }
    }

    typeMatch(mesh: Mesh | undefined, shape: Shape): boolean {
        if (!mesh) {
            return false;
        }
        const geometryId = (shape as unknown as ShapeDebugData).geometryId;
        const geo = mesh.geometry;
        return (
            (geo instanceof SphereGeometry && shape instanceof Sphere) ||
            (geo instanceof BoxGeometry && shape instanceof Box) ||
            (geo instanceof PlaneGeometry && shape instanceof Plane) ||
            (geo.id === geometryId && shape instanceof ConvexPolyhedron) ||
            (geo.id === geometryId && shape instanceof Trimesh) ||
            (geo.id === geometryId && shape instanceof Heightfield)
        );
    }

    createMesh(shape: Shape): Mesh | undefined {
        let mesh: Mesh | undefined;

        const shapeDebugData = shape as unknown as ShapeDebugData;
        const material = shapeDebugData.debugColor
            ? CannonDebugRenderer.materialByColor.get(shapeDebugData.debugColor)
            : this.defaultMaterial;

        const geometry = new BufferGeometry();
        const geo = new BufferGeometry();
        const v0 = this.tmpVec0;
        const v1 = this.tmpVec1;
        const v2 = this.tmpVec2;
        if (shape instanceof Sphere) {
            mesh = new Mesh(this.sphereGeometry, material);
        } else if (shape instanceof Box) {
            mesh = new Mesh(this.boxGeometry, material);
        } else if (shape instanceof Plane) {
            mesh = new Mesh(this.planeGeometry, material);
        } else if (shape instanceof ConvexPolyhedron) {
            // Create mesh
            const positions: number[] = [];
            const addFace = (a: Vec3, b: Vec3, c: Vec3): void => {
                positions.push(a.x, a.y, a.z);
                positions.push(b.x, b.y, b.z);
                positions.push(c.x, c.y, c.z);
            };
            for (let i = 0; i < shape.faces.length; i++) {
                const face = shape.faces[i];
                const normal = shape.faceNormals[i];

                // add triangles
                const a = shape.vertices[face[0]];

                for (let j = 1; j < face.length - 1; j++) {
                    const b = shape.vertices[face[j]];
                    const c = shape.vertices[face[j + 1]];

                    addFace(a, b, c);

                    const center = v0
                        .copy(a)
                        .vadd(b)
                        .vadd(c)
                        .scale(1.0 / 3);
                    const centerWithNormal = v1.copy(center).vadd(normal);
                    const centerWithNormalA = v2.copy(b).vsub(a).cross(normal);
                    centerWithNormalA.normalize();
                    addFace(center, centerWithNormal, centerWithNormalA.vadd(center));

                    const centerWithNormalB = v2.copy(c).vsub(b).cross(normal);
                    centerWithNormalB.normalize();
                    addFace(center, centerWithNormal, centerWithNormalB.vadd(center));

                    const centerWithNormalC = v2.copy(a).vsub(c).cross(normal);
                    centerWithNormalC.normalize();
                    addFace(center, centerWithNormal, centerWithNormalC.vadd(center));

                    addFace(center, v1.copy(center).vadd(normal.scale(2)), center);
                }
            }
            geo.setAttribute('position', new BufferAttribute(new Float32Array(positions), 3));
            geo.computeBoundingSphere();
            geo.computeVertexNormals();

            mesh = new Mesh(geo, material);
            shapeDebugData.geometryId = geo.id;
        } else if (shape instanceof Trimesh) {
            const positions: number[] = [];
            for (let i = 0; i < shape.indices.length / 3; i++) {
                shape.getTriangleVertices(i, v0, v1, v2);
                positions.push(v0.x, v0.y, v0.z);
                positions.push(v1.x, v1.y, v1.z);
                positions.push(v2.x, v2.y, v2.z);
            }
            geo.setAttribute('position', new BufferAttribute(new Float32Array(positions), 3));
            geometry.computeBoundingSphere();
            geometry.computeVertexNormals();
            mesh = new Mesh(geometry, material);
            shapeDebugData.geometryId = geo.id;
        } else if (shape instanceof Heightfield) {
            const positions: number[] = [];
            for (let xi = 0; xi < shape.data.length - 1; xi++) {
                for (let yi = 0; yi < shape.data[xi].length - 1; yi++) {
                    for (let k = 0; k < 2; k++) {
                        shape.getConvexTrianglePillar(xi, yi, k === 0);
                        v0.copy(shape.pillarConvex.vertices[0]);
                        v1.copy(shape.pillarConvex.vertices[1]);
                        v2.copy(shape.pillarConvex.vertices[2]);
                        v0.vadd(shape.pillarOffset, v0);
                        v1.vadd(shape.pillarOffset, v1);
                        v2.vadd(shape.pillarOffset, v2);
                        positions.push(v0.x, v0.y, v0.z);
                        positions.push(v1.x, v1.y, v1.z);
                        positions.push(v2.x, v2.y, v2.z);
                    }
                }
            }
            geometry.computeBoundingSphere();
            geometry.computeVertexNormals();
            mesh = new Mesh(geometry, material);
            shapeDebugData.geometryId = geo.id;
        }

        if (mesh && mesh.geometry) {
            this.scene.add(mesh);
            mesh.name = `Body(${shape.body?.id}):Debug`;
        }

        return mesh;
    }

    scaleMesh(mesh: Mesh, shape: Shape): void {
        if (shape instanceof Sphere) {
            const radius = shape.radius;
            mesh.scale.set(radius, radius, radius);
        } else if (shape instanceof Box) {
            mesh.scale.set(shape.halfExtents.x, shape.halfExtents.y, shape.halfExtents.z);
            mesh.scale.multiplyScalar(2);
        } else if (shape instanceof ConvexPolyhedron) {
            mesh.scale.set(1, 1, 1);
        } else if (shape instanceof Trimesh) {
            mesh.scale.set(shape.scale.x, shape.scale.y, shape.scale.z);
        } else if (shape instanceof Heightfield) {
            mesh.scale.set(1, 1, 1);
        }
    }

    static setShapeColor(shape: Shape, color: number): void {
        if (!CannonDebugRenderer.materialByColor.has(color)) {
            CannonDebugRenderer.materialByColor.set(
                color,
                new MeshBasicMaterial({ color: color, wireframe: true, fog: false })
            );
        }
        (shape as unknown as ShapeDebugData).debugColor = color;
    }
}
