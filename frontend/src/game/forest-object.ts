import {
    BufferAttribute,
    BufferGeometry,
    Euler,
    FrontSide,
    InstancedMesh,
    Material,
    Matrix4,
    Mesh,
    Object3D,
    Quaternion,
    Vector3,
} from 'three';
import { ForestData } from './entities/world/forest-data';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { addCredit } from '@/temp-util';
import { createToast } from 'mosha-vue-toastify';

const HEIGHT_RESOLUTION = 128;

interface BambooModel {
    availableHeightOffsets: Float64Array;
    leaf: Mesh<BufferGeometry, Material>;
    stem: Mesh<BufferGeometry, Material>;
}

export class ForestObject extends Object3D {
    private bambooModel?: BambooModel;
    private forestData?: ForestData;

    constructor(private readonly worldWidth: number, private readonly worldDepth: number) {
        super();

        this.name = 'Forest';

        new GLTFLoader()
            .setPath('/assets/bamboo/')
            .loadAsync('bamburro.glb')
            .then((gltf) => {
                addCredit({
                    thing: {
                        text: 'Bamboo model',
                    },
                    author: { text: 'Sleepy Fox', url: 'https://the-sleepy-fox.itch.io/' },
                });

                let stem: Mesh<BufferGeometry, Material> | undefined;
                let leaf: Mesh<BufferGeometry, Material> | undefined;
                const availableHeightOffsets = new Float64Array(HEIGHT_RESOLUTION);
                gltf.scene.traverse((object) => {
                    if (!(object instanceof Mesh) || !(object.geometry instanceof BufferGeometry)) {
                        return;
                    }
                    if (object.name === 'CutoffPlanes') {
                        this.calculateAvailableHeightOffsets(object.geometry, availableHeightOffsets);
                    } else if (object.material instanceof Material) {
                        if (object.material.name === 'Stem') {
                            stem = object;
                        } else if (object.material.name === 'Leaf') {
                            leaf = object;
                        }
                    }
                });
                if (!stem) {
                    createToast('The Bamboo model does not contain a Stem mesh', { type: 'danger', showIcon: true });
                    return;
                }
                if (!leaf) {
                    createToast('The Bamboo model does not contain a Leaf mesh', { type: 'danger', showIcon: true });
                    return;
                }

                this.bambooModel = {
                    availableHeightOffsets: availableHeightOffsets,
                    stem,
                    leaf,
                };
                this.generateIfPossible();
            });
    }

    setForestData(forestData: ForestData): void {
        this.forestData = forestData;
        this.generateIfPossible();
    }

    generateIfPossible(): void {
        if (!this.bambooModel || !this.forestData) {
            return;
        }

        const xDeltas = [-this.worldWidth, 0, this.worldWidth];
        const zDeltas = [-this.worldDepth, 0, this.worldDepth];
        const bambooCount = this.forestData.length * xDeltas.length * zDeltas.length;

        const stems = new InstancedMesh(this.bambooModel.stem.geometry, this.bambooModel.stem.material, bambooCount);
        stems.name = 'Stems';
        stems.matrixAutoUpdate = false;
        stems.material.side = FrontSide;

        const leaves = new InstancedMesh(this.bambooModel.leaf.geometry, this.bambooModel.leaf.material, bambooCount);
        leaves.name = 'Leaves';
        leaves.matrixAutoUpdate = false;

        const matrix = new Matrix4();
        const translation = new Vector3();
        const rotation = new Quaternion();
        const rotationEuler = new Euler();
        const scale = new Vector3(2, 1, 2);

        let meshIndex = 0;
        const rotations = new Float64Array(this.forestData.length);
        for (let i = 0; i < this.forestData.length; i++) {
            rotations[i] = Math.random() * Math.PI * 2;
        }
        for (const deltaX of xDeltas) {
            for (const deltaZ of zDeltas) {
                for (let i = 0; i < this.forestData.length; i++) {
                    translation.set(
                        (this.forestData.plantX[i] - 0.5) * this.worldWidth + deltaX,
                        this.bambooModel.availableHeightOffsets[this.forestData.plantHeight[i]],
                        (this.forestData.plantZ[i] - 0.5) * this.worldDepth + deltaZ
                    );
                    rotationEuler.y = rotations[i];
                    rotation.setFromEuler(rotationEuler);
                    matrix.compose(translation, rotation, scale);

                    for (const mesh of [stems, leaves]) {
                        mesh.setMatrixAt(meshIndex, matrix);
                    }
                    meshIndex++;
                }
            }
        }

        this.attach(stems);
        this.attach(leaves);
    }

    private performMitosis(
        forestData: ForestData,
        sourceGeometry: BufferGeometry,
        availableHeightOffsets: Float64Array
    ): BufferGeometry {
        sourceGeometry = sourceGeometry.toNonIndexed();
        const tmpGeometry = new BufferGeometry();
        const attributeMap = new Map<string, [number[], number]>();

        for (let i = 0; i < forestData.length; i++) {
            const x = (forestData.plantX[i] - 0.5) * this.worldWidth;
            const z = (forestData.plantZ[i] - 0.5) * this.worldDepth;
            const y = availableHeightOffsets[forestData.plantHeight[i]];
            tmpGeometry.copy(sourceGeometry);
            tmpGeometry.translate(x, y, z);
            for (const key in tmpGeometry.attributes) {
                let attributeValue = attributeMap.get(key);
                if (!attributeValue) {
                    attributeValue = [[], tmpGeometry.attributes[key].itemSize];
                    attributeMap.set(key, attributeValue);
                }
                this.addAll(tmpGeometry.attributes[key].array, attributeValue[0]);
            }
        }
        const result = new BufferGeometry();
        for (const entry of attributeMap.entries()) {
            result.setAttribute(entry[0], new BufferAttribute(new Float32Array(entry[1][0]), entry[1][1]));
        }
        tmpGeometry.dispose();
        return result;
    }

    private addAll<T>(from: ArrayLike<T>, to: T[]) {
        for (let i = 0; i < from.length; i++) {
            to.push(from[i]);
        }
    }

    private calculateAvailableHeightOffsets(cutoffPlanes: BufferGeometry, availableHeightOffsets: Float64Array): void {
        const heightSet = new Set<number>();
        const positionAttribute = cutoffPlanes.getAttribute('position');
        for (let i = 0; i < positionAttribute.count; i++) {
            heightSet.add(positionAttribute.getY(i));
        }
        if (heightSet.size % 2 === 1) {
            createToast('The number of bamboo cutoff planes is odd!', { type: 'warning', showIcon: true });
            return;
        }
        const heights = [...heightSet.values()].sort((a, b) => a - b);
        const minHeight = heights[0];
        const maxHeight = heights[heights.length - 1];
        const totalHeightRange = maxHeight - minHeight;
        const heightPerStep = totalHeightRange / HEIGHT_RESOLUTION;
        let isBetweenValidHeights = false;
        for (let height = minHeight, i = 0, index = 0; i < HEIGHT_RESOLUTION; i++, height += heightPerStep) {
            while (height > heights[index + 1]) {
                index++;
                isBetweenValidHeights = !isBetweenValidHeights;
            }
            if (index >= heights.length - 1) {
                createToast('Reached index ' + index, { type: 'warning' });
            }
            availableHeightOffsets[i] = isBetweenValidHeights
                ? height - heights[index] < heights[index + 1] - height
                    ? heights[index]
                    : heights[index + 1]
                : height;
        }
        for (let i = 0; i < HEIGHT_RESOLUTION; i++) {
            availableHeightOffsets[i] *= -1;
        }
        availableHeightOffsets.reverse();
    }
}
