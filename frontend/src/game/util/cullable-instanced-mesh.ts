import { Box3, Box3Helper, Frustum, InstancedMesh, Matrix4, Object3D, Quaternion, Vector3 } from 'three';

// For temporary calculations
const position = new Vector3();
const rotation = new Quaternion();
const scale = new Vector3();
const matrix4 = new Matrix4();
const otherMatrix4 = new Matrix4();
const boundingBox = new Box3();
const boundingBoxMin = new Vector3();
const boundingBoxMax = new Vector3();

// Discovered through trial and error
const FRUSTUM_FAR_PLANE_INDEX = 4;
const FRUSTUM_NEAR_PLANE_INDEX = 5;

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

    /**
     * Create a preview of the bounding box of every instance to help with debugging.
     */
    createBoundingBoxes(): void {
        for (let i = 0; i < this.count; i++) {
            this.primaryMesh.getMatrixAt(i, matrix4);
            this.attach(
                new Box3Helper(new Box3().copy(this.getBoundingBoxAt(position.setFromMatrixPosition(matrix4))))
            );
        }
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
    cull(frustum: Frustum, farPlaneTransition: number, nearPlaneTransition?: number): number {
        let visible = 0;
        let needsUpdate = false;
        for (let i = 0; i < this.count; i++) {
            this.primaryMesh.getMatrixAt(i, matrix4);
            position.setFromMatrixPosition(matrix4);
            if (!frustum.intersectsBox(this.getBoundingBoxAt(position))) {
                continue;
            }
            const farPlaneDistance = frustum.planes[FRUSTUM_FAR_PLANE_INDEX].distanceToPoint(position);
            let horizontalScale = farPlaneDistance > 0 ? farPlaneDistance / farPlaneTransition : 0;

            if (horizontalScale > 0 && nearPlaneTransition != null) {
                const nearPlaneDistance = frustum.planes[FRUSTUM_NEAR_PLANE_INDEX].distanceToPoint(position);
                horizontalScale = Math.min(
                    horizontalScale,
                    nearPlaneDistance > 0 ? nearPlaneDistance / farPlaneTransition : 0
                );
            }

            if (horizontalScale <= 0) {
                continue;
            }

            this.updateHorizontalScale(matrix4, position, Math.min(horizontalScale, 1));
            for (const mesh of this.meshes) {
                mesh.setMatrixAt(i, matrix4);
            }
            if (i > visible) {
                this.primaryMesh.getMatrixAt(visible, otherMatrix4);
                for (const mesh of this.meshes) {
                    mesh.setMatrixAt(i, otherMatrix4);
                    mesh.setMatrixAt(visible, matrix4);
                }
            }
            needsUpdate = true;
            visible++;
        }
        for (const mesh of this.meshes) {
            mesh.count = visible;
            mesh.instanceMatrix.needsUpdate = needsUpdate;
        }

        return visible;
    }

    private updateHorizontalScale(matrix: Matrix4, position: Vector3, newScale: number): void {
        matrix.decompose(position, rotation, scale);
        matrix.compose(position, rotation, scale.set(newScale, 1, newScale));
    }

    private getBoundingBoxAt(center: Vector3) {
        boundingBox.set(
            boundingBoxMin.copy(center).add(this.boundingBoxMinOffset),
            boundingBoxMax.copy(center).add(this.boundingBoxMaxOffset)
        );
        return boundingBox;
    }
}
