import { Box3, Box3Helper, Frustum, InstancedMesh, Matrix4, Object3D, Vector3 } from 'three';

const position = new Vector3();
const matrix4 = new Matrix4();
const otherMatrix4 = new Matrix4();
const boundingBox = new Box3();
const boundingBoxMin = new Vector3();
const boundingBoxMax = new Vector3();

export class CullableInstancedMesh extends Object3D {
    private readonly boundingBoxMinOffset: Vector3;
    private readonly boundingBoxMaxOffset: Vector3;
    readonly count: number;
    private readonly primaryMesh: InstancedMesh;
    private readonly meshes: InstancedMesh[];

    constructor(meshOrMeshes: InstancedMesh[] | InstancedMesh, horizontalPadding: number, height: number) {
        super();
        this.meshes = [meshOrMeshes].flat();
        this.primaryMesh = this.meshes[0];
        this.count = this.primaryMesh.count;
        for (const mesh of this.meshes) {
            if (mesh.count !== this.count) {
                throw new Error(`One of the instanced meshes has a count of ${mesh.count} instead of ${this.count}`);
            }
            this.attach(mesh);
        }
        this.boundingBoxMinOffset = new Vector3(-horizontalPadding, 0, -horizontalPadding);
        this.boundingBoxMaxOffset = new Vector3(horizontalPadding, height, horizontalPadding);
    }

    showBoundingBoxes(): void {
        for (let i = 0; i < this.count; i++) {
            this.primaryMesh.getMatrixAt(i, matrix4);
            this.attach(new Box3Helper(new Box3().copy(this.getBoundingBoxAt(matrix4))));
        }
    }

    cull(frustum: Frustum): number {
        let visible = 0;
        for (let i = 0; i < this.count; i++) {
            this.primaryMesh.getMatrixAt(i, matrix4);
            if (!frustum.intersectsBox(this.getBoundingBoxAt(matrix4))) {
                continue;
            }
            if (i > visible) {
                this.primaryMesh.getMatrixAt(visible, otherMatrix4);
                for (const mesh of this.meshes) {
                    mesh.setMatrixAt(i, otherMatrix4);
                    mesh.setMatrixAt(visible, matrix4);
                    mesh.instanceMatrix.needsUpdate = true;
                }
            }
            visible++;
        }
        for (const mesh of this.meshes) {
            mesh.count = visible;
        }

        return visible;
    }

    private getBoundingBoxAt(matrix: Matrix4) {
        position.setFromMatrixPosition(matrix);
        boundingBox.set(
            boundingBoxMin.copy(position).add(this.boundingBoxMinOffset),
            boundingBoxMax.copy(position).add(this.boundingBoxMaxOffset)
        );
        return boundingBox;
    }
}
