import { InstancedMesh, Mesh, Object3D, PlaneBufferGeometry } from 'three';
import { ForestCellData } from './forest-cell-data';

const DIRT_PLANE_OFFSET = 0.1;

export class ForestCellObject extends Object3D {
    debugData = '';

    private readonly plantContainer: Object3D;
    private readonly dirtPlane: Mesh;

    readonly count: number;

    static spawnAttempts = 0;

    constructor(private readonly instancedMeshes: InstancedMesh[], cellWidth: number, cellDepth: number) {
        super();
        this.name = 'ForestCell';

        this.dirtPlane = new Mesh(new PlaneBufferGeometry(cellWidth, cellDepth));
        this.dirtPlane.rotateX(-Math.PI / 2);
        this.dirtPlane.position.y = DIRT_PLANE_OFFSET;
        this.dirtPlane.receiveShadow = true;
        this.attach(this.dirtPlane);

        this.plantContainer = new Object3D();
        for (const mesh of instancedMeshes) {
            mesh.position.set(-cellWidth / 2, 0, -cellDepth / 2);
            this.plantContainer.attach(mesh);
        }
        this.attach(this.plantContainer);

        let count = 0;
        for (let i = 0; i < instancedMeshes.length; i += 2) {
            count += instancedMeshes[i].count;
        }
        this.count = count;
    }

    applyData(data: ForestCellData): void {
        this.name = data.name;
        this.position.copy(data.position);
        this.dirtPlane.material = data.dirtMaterial;
        this.plantContainer.visible = data.plantContainerVisible;
        if (this.plantContainer.visible) {
            for (let i = 0; i < data.allPlantMatrices.length; i++) {
                this.instancedMeshes[i * 2].instanceMatrix = data.allPlantMatrices[i];
                this.instancedMeshes[i * 2].count = data.countPerPlantType[i];
                this.instancedMeshes[i * 2 + 1].instanceMatrix = data.allPlantMatrices[i];
                this.instancedMeshes[i * 2 + 1].count = data.countPerPlantType[i];
            }
            this.plantContainer.scale.set(data.plantScaleX, 1, data.plantScaleZ);
        }
        this.visible = true;
    }

    setReceiveShadow(receiveShadow: boolean): void {
        for (const mesh of this.instancedMeshes) {
            mesh.receiveShadow = receiveShadow;
        }
    }
}
