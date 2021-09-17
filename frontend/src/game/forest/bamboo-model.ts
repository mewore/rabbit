import {
    BufferGeometry,
    Color,
    Euler,
    FrontSide,
    Group,
    InstancedMesh,
    Material,
    Matrix4,
    Mesh,
    MeshBasicMaterial,
    MeshStandardMaterial,
    Quaternion,
    Vector3,
} from 'three';
import { CullableInstancedMesh } from '../util/cullable-instanced-mesh';
import { ForestData } from '../entities/world/forest-data';
import { createToast } from 'mosha-vue-toastify';

const PLANT_PADDING = 15;
const PLANT_HEIGHT = 100;

const MAX_LEANING_ANGLE = 0.05 * Math.PI;
const DEFAULT_MAX_DEPTH = 5.0;

export class BambooModel {
    constructor(
        readonly maxHeight: number,
        private readonly stemMesh: Mesh<BufferGeometry, Material>,
        private readonly leafMesh: Mesh<BufferGeometry, Material>,
        public maxDepth: number = DEFAULT_MAX_DEPTH
    ) {}

    static fromObject(bambooObject: Group, isDummy = false): BambooModel | undefined {
        let stem: Mesh<BufferGeometry, MeshStandardMaterial | MeshBasicMaterial> | undefined;
        let leaf: Mesh<BufferGeometry, MeshStandardMaterial | MeshBasicMaterial> | undefined;
        bambooObject.traverse((child) => {
            if (!(child instanceof Mesh) || !(child.geometry instanceof BufferGeometry)) {
                return;
            }
            if (child.material instanceof MeshStandardMaterial) {
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
        if (isDummy) {
            stem.material = new MeshBasicMaterial({ color: new Color(0x003300) });
            leaf.material = new MeshBasicMaterial({
                color: new Color(0x003300),
                alphaMap: leaf.material.alphaMap,
                transparent: true,
            });
        }
        const firstDigitIndex = /\d/.exec(bambooObject.name)?.index;
        if (firstDigitIndex == null) {
            createToast(
                `The Bamboo model "${bambooObject.name}" does not contain a maximum height at the end of its name`,
                {
                    type: 'danger',
                    showIcon: true,
                }
            );
            return;
        }
        return new BambooModel(parseInt(bambooObject.name.substring(firstDigitIndex)), stem, leaf);
    }

    makeInstances(
        forestData: ForestData,
        indices: number[],
        offsets: Vector3[],
        worldWidth: number,
        worldDepth: number,
        tallness: number
    ): CullableInstancedMesh {
        const bambooCount = indices.length * offsets.length;

        const stems = new InstancedMesh(this.stemMesh.geometry, this.stemMesh.material, bambooCount);
        stems.name = `Bamboo:${this.maxHeight}:Stems`;
        stems.material.side = FrontSide;

        const leaves = new InstancedMesh(this.leafMesh.geometry, this.leafMesh.material, bambooCount);
        leaves.name = `Bamboo:${this.maxHeight}:Leaves`;

        const matrix = new Matrix4();
        const translation = new Vector3();
        const rotation = new Quaternion();
        const rotationEuler = new Euler();
        const scale = new Vector3(1, 1, 1);

        let instanceIndex = 0;
        for (const i of indices) {
            rotationEuler.x = Math.random() * Math.random() * MAX_LEANING_ANGLE * tallness;
            rotationEuler.y = Math.random() * Math.PI * 2;
            rotationEuler.z = Math.random() * Math.random() * MAX_LEANING_ANGLE * tallness;
            rotation.setFromEuler(rotationEuler);
            const y = -Math.random() * this.maxDepth;
            for (const offset of offsets) {
                translation
                    .set((forestData.plantX[i] - 0.5) * worldWidth, y, (forestData.plantZ[i] - 0.5) * worldDepth)
                    .add(offset);
                matrix.compose(translation, rotation, scale);

                for (const mesh of [stems, leaves]) {
                    mesh.setMatrixAt(instanceIndex, matrix);
                }
                instanceIndex++;
            }
        }

        return new CullableInstancedMesh([stems, leaves], PLANT_PADDING, PLANT_HEIGHT);
    }
}
