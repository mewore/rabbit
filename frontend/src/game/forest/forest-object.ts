import { Camera, Frustum, Group, Matrix4, Object3D, OrthographicCamera, PerspectiveCamera, Vector3 } from 'three';
import { BambooModel } from './bamboo-model';
import { CullableInstancedMesh } from '../util/cullable-instanced-mesh';
import { ForestData } from '../entities/world/forest-data';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { Updatable } from '../updatable';
import { addCredit } from '@/temp-util';
import { createToast } from 'mosha-vue-toastify';

const matrix4 = new Matrix4();
const frustum = new Frustum();
const tmpOrthographicCamera = new OrthographicCamera(0, 0, 0, 0);
const tmpPerspectiveCamera = new PerspectiveCamera();

export class ForestObject extends Object3D implements Updatable {
    private bambooModels?: BambooModel[];
    private dummyBambooModels: BambooModel[] = [];
    private forestData?: ForestData;
    private readonly meshes: CullableInstancedMesh[] = [];
    private readonly dummyMeshes: CullableInstancedMesh[] = [];
    camera?: Camera;

    private currentTotalPlants = 0;
    private currentRenderedDetailedPlants = 0;
    private currentRenderedDummyPlants = 0;

    constructor(
        private readonly worldWidth: number,
        private readonly worldDepth: number,
        private readonly offsets: Vector3[] = [new Vector3()]
    ) {
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

                const bambooObjects: Group[] = [];
                const dummyBambooObjects: Group[] = [];
                gltf.scene.traverse((object) => {
                    if (object instanceof Group && object.name.startsWith('Bamboo')) {
                        bambooObjects.push(object);
                    } else if (object instanceof Group && object.name.startsWith('DummyBamboo')) {
                        dummyBambooObjects.push(object);
                    }
                });
                const bambooModels: BambooModel[] = [];
                const dummyBambooModels: BambooModel[] = [];
                const containerTypes: [Group[], BambooModel[]][] = [
                    [bambooObjects, bambooModels],
                    [dummyBambooObjects, dummyBambooModels],
                ];
                for (const containers of containerTypes) {
                    for (const bambooObject of containers[0]) {
                        const bambooModel = BambooModel.fromObject(bambooObject);
                        if (bambooModel) {
                            containers[1].push(bambooModel);
                        }
                    }
                }
                if (bambooModels.length === 0) {
                    createToast('There are no bamboo models', { type: 'danger', showIcon: true });
                    return;
                }

                this.bambooModels = bambooModels.sort((first, second) => first.maxHeight - second.maxHeight);
                this.bambooModels[0].maxDepth = 0;

                this.dummyBambooModels = dummyBambooModels.sort((first, second) => first.maxHeight - second.maxHeight);
                this.dummyBambooModels[0].maxDepth = 0;

                this.generateIfPossible();
            });
    }

    get totalPlants(): number {
        return this.currentTotalPlants;
    }

    get renderedDetailedPlants(): number {
        return this.currentRenderedDetailedPlants;
    }

    get renderedDummyPlants(): number {
        return this.currentRenderedDummyPlants;
    }

    setForestData(forestData: ForestData): void {
        this.forestData = forestData;
        this.generateIfPossible();
    }

    update(): void {
        if (!this.camera || !this.meshes.length) {
            return;
        }

        if (!(this.camera instanceof OrthographicCamera || this.camera instanceof PerspectiveCamera)) {
            throw new Error(
                'The camera should be either an orthographic or a perspective one. Instead, it is of type "' +
                    this.camera.type
            );
        }

        const crossfadeRatio = 0.1;
        const fadeOutRatio = 0.1;
        const detailedRatio = 0.4;

        this.camera.updateMatrixWorld();
        const tmpCamera =
            this.camera instanceof OrthographicCamera
                ? tmpOrthographicCamera.copy(this.camera)
                : tmpPerspectiveCamera.copy(this.camera);
        const splitDistance = this.camera.near * (1.0 - detailedRatio) + this.camera.far * detailedRatio;
        const crossfadeDistance =
            Math.min(splitDistance - tmpCamera.near, tmpCamera.far - splitDistance) * crossfadeRatio;
        tmpCamera.far = splitDistance + crossfadeDistance / 2;
        tmpCamera.updateProjectionMatrix();
        frustum.setFromProjectionMatrix(
            matrix4.multiplyMatrices(tmpCamera.projectionMatrix, tmpCamera.matrixWorldInverse)
        );

        // The nearby ones are detailed
        this.currentTotalPlants = 0;
        this.currentRenderedDetailedPlants = 0;
        for (const mesh of this.meshes) {
            this.currentRenderedDetailedPlants += mesh.cull(frustum, crossfadeDistance);
            this.currentTotalPlants += mesh.count;
        }

        // The rest are dummies
        const fadeOutDistance = (tmpCamera.far - tmpCamera.near) * fadeOutRatio;
        tmpCamera.near = splitDistance - crossfadeDistance / 2;
        tmpCamera.far = this.camera.far;
        tmpCamera.updateProjectionMatrix();
        tmpCamera.updateMatrixWorld();
        frustum.setFromProjectionMatrix(
            matrix4.multiplyMatrices(tmpCamera.projectionMatrix, tmpCamera.matrixWorldInverse)
        );
        this.currentRenderedDummyPlants = 0;
        for (const mesh of this.dummyMeshes) {
            this.currentRenderedDummyPlants += mesh.cull(frustum, crossfadeDistance, fadeOutDistance);
        }
    }

    private generateIfPossible(): void {
        if (!this.bambooModels || !this.forestData || this.children.length > 0) {
            return;
        }

        const indicesPerModel: number[][] = this.bambooModels.map(() => []);
        const indicesPerDummyModel: number[][] = this.dummyBambooModels.map(() => []);
        for (let i = 0; i < this.forestData.length; i++) {
            for (let j = 0; j < this.bambooModels.length; j++) {
                if (this.bambooModels[j].maxHeight > this.forestData.plantHeight[i]) {
                    indicesPerModel[j].push(i);
                    break;
                }
            }
            for (let j = 0; j < this.dummyBambooModels.length; j++) {
                if (this.dummyBambooModels[j].maxHeight > this.forestData.plantHeight[i]) {
                    indicesPerDummyModel[j].push(i);
                    break;
                }
            }
        }
        for (let i = 0; i < this.bambooModels.length; i++) {
            const instancedMesh = this.bambooModels[i].makeInstances(
                this.forestData,
                indicesPerModel[i],
                this.offsets,
                this.worldWidth,
                this.worldDepth,
                (i + 1) / this.bambooModels.length
            );
            if (instancedMesh.count > 0) {
                this.attach(instancedMesh);
                this.meshes.push(instancedMesh);
                this.currentTotalPlants += instancedMesh.count;
            }
        }
        for (let i = 0; i < this.dummyBambooModels.length; i++) {
            const instancedDummyMesh = this.dummyBambooModels[i].makeInstances(
                this.forestData,
                indicesPerDummyModel[i],
                this.offsets,
                this.worldWidth,
                this.worldDepth,
                (i + 1) / this.dummyBambooModels.length
            );
            if (instancedDummyMesh.count > 0) {
                this.attach(instancedDummyMesh);
                this.dummyMeshes.push(instancedDummyMesh);
            }
        }
        this.currentRenderedDetailedPlants = this.currentRenderedDummyPlants = this.currentTotalPlants;
    }
}
