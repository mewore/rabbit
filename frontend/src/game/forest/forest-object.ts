import {
    Camera,
    Frustum,
    Group,
    InstancedMesh,
    Material,
    Matrix4,
    Object3D,
    OrthographicCamera,
    PerspectiveCamera,
    TextureLoader,
    Vector2,
    Vector3,
} from 'three';
import { BambooModel } from './bamboo-model';
import { ForestCell } from './forest-cell';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { Input } from '../util/input';
import { MazeMap } from '../entities/world/maze-map';
import { Updatable } from '../util/updatable';
import { addCredit } from '@/temp-util';
import { createToast } from 'mosha-vue-toastify';

const matrix4 = new Matrix4();
const frustum = new Frustum();
const tmpVector2 = new Vector2();
const tmpVector3 = new Vector3();
const min = new Vector2(Infinity, Infinity);
const max = new Vector2(-Infinity, -Infinity);
const oldCameraPosition = new Vector3();

const BASE_FOREST_UPDATE_RATE = 1;
const FOREST_UPDATE_PER_ROTATION = 0.3;
const FOREST_UPDATE_PER_MOVEMENT = 0.3;

const BAMBOO_MODEL_LOCATION = '/assets/bamboo/';
const BAMBOO_MODEL_FILENAME = 'bamburro.glb';

export class ForestObject extends Object3D implements Updatable {
    private bambooModels?: BambooModel[];
    private dummyBambooModels: BambooModel[] = [];
    private mapData?: MazeMap;
    private readonly cells: ForestCell[] = [];

    private cellGrid: (ForestCell | undefined)[][] | undefined;

    private forestUpdateTimeout = 0;
    private readonly oldCameraPosition = new Vector3();
    camera?: Camera;

    private currentTotalPlants = 0;
    private currentRenderedDetailedPlants = 0;
    private currentRenderedDummyPlants = 0;

    private visibleCells: ForestCell[] = [];

    private receivingShadows = false;
    visiblePlants = 1.0;

    constructor(
        private readonly worldWidth: number,
        private readonly worldDepth: number,
        private readonly input: Input
    ) {
        super();

        this.name = 'Forest';

        new GLTFLoader()
            .setPath(BAMBOO_MODEL_LOCATION)
            .loadAsync(BAMBOO_MODEL_FILENAME)
            .then((gltf) => {
                addCredit({
                    thing: {
                        text: 'Bamboo model',
                        url: BAMBOO_MODEL_LOCATION + BAMBOO_MODEL_FILENAME,
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

    setReceiveShadow(receiveShadow: boolean): void {
        if (this.receivingShadows === receiveShadow) {
            return;
        }
        this.receivingShadows = receiveShadow;
        for (const cell of this.cells) {
            cell.setReceiveShadow(receiveShadow);
        }
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

    setMapData(mapData: MazeMap): void {
        this.mapData = mapData;
        this.generateIfPossible();
    }

    beforePhysics(): void {}

    afterPhysics(): void {}

    update(): void {}

    beforeRender(delta: number): void {
        if (!this.camera || !this.cells.length || !this.cellGrid || !this.mapData) {
            return;
        }

        this.forestUpdateTimeout -=
            delta * BASE_FOREST_UPDATE_RATE +
            (Math.abs(this.input.lookRight) + Math.abs(this.input.lookDown)) * FOREST_UPDATE_PER_ROTATION +
            oldCameraPosition.distanceToSquared(this.camera.position) * FOREST_UPDATE_PER_MOVEMENT;
        this.oldCameraPosition.copy(this.camera.position);
        if (this.forestUpdateTimeout > 0) {
            return;
        }

        this.forestUpdateTimeout = 1;

        if (!(this.camera instanceof OrthographicCamera || this.camera instanceof PerspectiveCamera)) {
            throw new Error(
                'The camera should be either an orthographic or a perspective one. Instead, it is of type "' +
                    this.camera.type
            );
        }

        const fadeOutPortion = 0.1;
        const viewDistance = this.camera.far - this.camera.near;

        this.camera.updateMatrixWorld();
        frustum.setFromProjectionMatrix(
            matrix4.multiplyMatrices(this.camera.projectionMatrix, this.camera.matrixWorldInverse)
        );

        this.currentRenderedDetailedPlants = 0;

        // Look only at the cells around the bounding box of the camera frustum.
        const height = this.cellGrid.length;
        const width = this.cellGrid[0].length;
        min.set(Infinity, Infinity);
        max.set(-Infinity, -Infinity);
        const nearToFarScale = this.camera.far / this.camera.near;
        for (const x of [-1, 1]) {
            for (const y of [-1, 1]) {
                tmpVector3.set(x, y, -1).unproject(this.camera);
                tmpVector2.set(tmpVector3.x, tmpVector3.z);
                min.min(tmpVector2);
                max.max(tmpVector2);
                tmpVector2
                    .set(tmpVector2.x - this.camera.position.x, tmpVector2.y - this.camera.position.z)
                    .multiplyScalar(nearToFarScale)
                    .set(tmpVector2.x + this.camera.position.x, tmpVector2.y + this.camera.position.z);
                min.min(tmpVector2);
                max.max(tmpVector2);
            }
        }
        const topRow = Math.floor((min.y / this.worldDepth + 0.5) * height) - 1;
        const bottomRow = Math.ceil((max.y / this.worldDepth + 0.5) * height) + 1;
        const leftColumn = Math.floor((min.x / this.worldWidth + 0.5) * width) - 1;
        const rightColumn = Math.ceil((max.x / this.worldWidth + 0.5) * width) + 1;

        for (const cell of this.visibleCells) {
            if (cell.row < topRow || cell.row > bottomRow || cell.column < leftColumn || cell.column > rightColumn) {
                cell.visible = false;
            }
        }
        this.visibleCells = [];

        let offsetX = 0;
        let offsetZ = 0;
        for (let i = topRow; i <= bottomRow; i++) {
            offsetZ = (i < 0 ? -this.worldDepth : 0) + (i >= height ? this.worldDepth : 0);
            for (let j = leftColumn; j <= rightColumn; j++) {
                const cell = this.cellGrid[this.mapData.wrapRow(i)][this.mapData.wrapColumn(j)];
                if (cell) {
                    offsetX = (j < 0 ? -this.worldWidth : 0) + (j >= width ? this.worldWidth : 0);
                    cell.reposition(offsetX, offsetZ);
                    this.currentRenderedDetailedPlants += cell.cull(
                        frustum,
                        fadeOutPortion,
                        viewDistance,
                        this.visiblePlants
                    );
                    if (cell.visible) {
                        this.visibleCells.push(cell);
                    }
                }
            }
        }
    }

    private generateIfPossible(): void {
        if (!this.bambooModels || !this.mapData || this.children.length > 0) {
            return;
        }

        const textureLoader = new TextureLoader();
        const dirtTexturePromise = textureLoader.loadAsync('./assets/dirt.jpg');

        let totalPlantCount = 0;
        const memorizedPlants: Map<number, InstancedMesh[]> = new Map();
        const memorizedDirtMaterials: Map<number, Material> = new Map();
        this.cellGrid = [];
        for (let i = 0; i < this.mapData.height; i++) {
            this.cellGrid.push([]);
            for (let j = 0; j < this.mapData.width; j++) {
                const cell = ForestCell.fromMapData(
                    this.mapData,
                    i,
                    j,
                    memorizedPlants,
                    memorizedDirtMaterials,
                    this.worldWidth,
                    this.worldDepth,
                    this.bambooModels,
                    dirtTexturePromise
                );
                if (cell) {
                    this.cells.push(cell);
                    totalPlantCount += cell.count;
                    this.attach(cell);
                    cell.setReceiveShadow(this.receivingShadows);
                }
                this.cellGrid[i].push(cell);
            }
        }
        this.currentTotalPlants = totalPlantCount;
    }
}
