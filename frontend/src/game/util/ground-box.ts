import { Body, Box, Material, Quaternion, Vec3 } from 'cannon-es';
import { BoxBufferGeometry, Mesh, MeshPhongMaterial } from 'three';

const MATERIAL = new MeshPhongMaterial({ color: 0x666666 });
const PHYSICS_MATERIAL = new Material({ friction: 0, restitution: 0 });

export class GroundBox extends Mesh<BoxBufferGeometry> {
    readonly body: Body;

    constructor(width: number, height: number, x: number, z: number, rotationY?: number) {
        super(new BoxBufferGeometry(width, height, width), MATERIAL);
        this.position.set(x, height / 2, z);
        if (rotationY != null) {
            this.rotateY(rotationY);
        }
        this.receiveShadow = true;
        this.castShadow = true;
        this.body = new Body({
            shape: new Box(new Vec3(width / 2, height / 2, width / 2)),
            position: new Vec3(x, height / 2, z),
            quaternion: rotationY != null ? new Quaternion().setFromEuler(0, rotationY, 0) : undefined,
            material: PHYSICS_MATERIAL,
        });
    }
}
