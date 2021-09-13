import {
    BufferGeometry,
    Euler,
    FrontSide,
    Group,
    InstancedMesh,
    Material,
    Matrix4,
    Mesh,
    Quaternion,
    Vector3,
} from 'three';
import { ForestData } from '../entities/world/forest-data';
import { createToast } from 'mosha-vue-toastify';

const MAX_LEANING_ANGLE = 0.1 * Math.PI;

export class BambooModel {
    constructor(
        readonly maxHeight: number,
        private readonly stemMesh: Mesh<BufferGeometry, Material>,
        private readonly leafMesh: Mesh<BufferGeometry, Material>
    ) {}

    static fromObject(bambooObject: Group): BambooModel | undefined {
        let stem: Mesh<BufferGeometry, Material> | undefined;
        let leaf: Mesh<BufferGeometry, Material> | undefined;
        bambooObject.traverse((child) => {
            if (!(child instanceof Mesh) || !(child.geometry instanceof BufferGeometry)) {
                return;
            }
            if (child.material instanceof Material) {
                if (child.material.name === 'Stem') {
                    stem = child;
                } else if (child.material.name === 'Leaf') {
                    leaf = child;
                }
            }
        });
        if (!stem) {
            createToast(`The Bamboo model "${bambooObject.name}" does not contain a mesh with the Stem material`, {
                type: 'danger',
                showIcon: true,
            });
            return;
        }
        if (!leaf) {
            createToast(`The Bamboo model "${bambooObject.name}" does not contain a mesh with the Leaf material`, {
                type: 'danger',
                showIcon: true,
            });
            return;
        }
        return new BambooModel(parseInt(bambooObject.name.substring('Bamboo'.length)), stem, leaf);
    }

    makeInstances(
        forestData: ForestData,
        indices: number[],
        xDeltas: number[],
        zDeltas: number[],
        worldWidth: number,
        worldDepth: number,
        tallness: number
    ): InstancedMesh[] {
        const bambooCount = indices.length * xDeltas.length * zDeltas.length;

        const stems = new InstancedMesh(this.stemMesh.geometry, this.stemMesh.material, bambooCount);
        stems.name = `Bamboo:${this.maxHeight}:Stems`;
        stems.matrixAutoUpdate = false;
        stems.material.side = FrontSide;

        const leaves = new InstancedMesh(this.leafMesh.geometry, this.leafMesh.material, bambooCount);
        leaves.name = `Bamboo:${this.maxHeight}:Leaves`;
        leaves.matrixAutoUpdate = false;

        const matrix = new Matrix4();
        const translation = new Vector3();
        const rotation = new Quaternion();
        const rotationEuler = new Euler();
        const scale = new Vector3(2, 1, 2);

        let meshIndex = 0;
        for (const i of indices) {
            rotationEuler.x = Math.random() * Math.random() * MAX_LEANING_ANGLE * tallness;
            rotationEuler.y = Math.random() * Math.PI * 2;
            rotationEuler.z = Math.random() * Math.random() * MAX_LEANING_ANGLE * tallness;
            rotation.setFromEuler(rotationEuler);
            for (const deltaX of xDeltas) {
                for (const deltaZ of zDeltas) {
                    translation.set(
                        (forestData.plantX[i] - 0.5) * worldWidth + deltaX,
                        0,
                        (forestData.plantZ[i] - 0.5) * worldDepth + deltaZ
                    );
                    matrix.compose(translation, rotation, scale);

                    for (const mesh of [stems, leaves]) {
                        mesh.setMatrixAt(meshIndex, matrix);
                    }
                    meshIndex++;
                }
            }
        }

        return [stems, leaves];
    }
}
