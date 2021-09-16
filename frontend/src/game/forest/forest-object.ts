import { Camera, Frustum, Group, Matrix4, Object3D } from 'three';
import { BambooModel } from './bamboo-model';
import { CullableInstancedMesh } from '../util/cullable-instanced-mesh';
import { ForestData } from '../entities/world/forest-data';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { Updatable } from '../updatable';
import { addCredit } from '@/temp-util';
import { createToast } from 'mosha-vue-toastify';

const matrix4 = new Matrix4();
const frustum = new Frustum();

export class ForestObject extends Object3D implements Updatable {
    private bambooModels?: BambooModel[];
    private forestData?: ForestData;
    private readonly meshes: CullableInstancedMesh[] = [];
    camera?: Camera;

    private currentTotalPlants = 0;
    private currentRenderedPlants = 0;

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

                const bambooObjects: Group[] = [];
                gltf.scene.traverse((object) => {
                    if (object instanceof Group && object.name.startsWith('Bamboo')) {
                        bambooObjects.push(object);
                    }
                });
                const bambooModels: BambooModel[] = [];
                for (const bambooObject of bambooObjects) {
                    const bambooModel = BambooModel.fromObject(bambooObject);
                    if (bambooModel) {
                        bambooModels.push(bambooModel);
                    }
                }
                if (bambooModels.length === 0) {
                    createToast('There are no bamboo models', { type: 'danger', showIcon: true });
                    return;
                }

                this.bambooModels = bambooModels.sort((first, second) => first.maxHeight - second.maxHeight);
                this.bambooModels[0].maxDepth = 0;
                this.generateIfPossible();
            });
    }

    get totalPlants(): number {
        return this.currentTotalPlants;
    }

    get renderedPlants(): number {
        return this.currentRenderedPlants;
    }

    setForestData(forestData: ForestData): void {
        this.forestData = forestData;
        this.generateIfPossible();
    }

    update(): void {
        if (!this.camera || !this.meshes.length) {
            return;
        }

        this.camera.updateMatrixWorld();
        frustum.setFromProjectionMatrix(
            matrix4.multiplyMatrices(this.camera.projectionMatrix, this.camera.matrixWorldInverse)
        );
        this.currentTotalPlants = 0;
        this.currentRenderedPlants = 0;
        for (const mesh of this.meshes) {
            this.currentRenderedPlants += mesh.cull(frustum);
            this.currentTotalPlants += mesh.count;
        }
    }

    private generateIfPossible(): void {
        if (!this.bambooModels || !this.forestData || this.children.length > 0) {
            return;
        }

        const xDeltas = [-this.worldWidth, 0, this.worldWidth];
        const zDeltas = [-this.worldDepth, 0, this.worldDepth];

        const indicesPerModel: number[][] = this.bambooModels.map(() => []);
        for (let i = 0; i < this.forestData.length; i++) {
            for (let j = 0; j < this.bambooModels.length; j++) {
                if (this.bambooModels[j].maxHeight > this.forestData.plantHeight[i]) {
                    indicesPerModel[j].push(i);
                    break;
                }
            }
        }
        for (let i = 0; i < this.bambooModels.length; i++) {
            const instancedMesh = this.bambooModels[i].makeInstances(
                this.forestData,
                indicesPerModel[i],
                xDeltas,
                zDeltas,
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
        this.currentRenderedPlants = this.currentTotalPlants;
    }
}
