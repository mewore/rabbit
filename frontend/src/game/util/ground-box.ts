import Ammo from 'ammo.js';
import { BoxBufferGeometry, Mesh, MeshPhongMaterial } from 'three';

const MATERIAL = new MeshPhongMaterial({ color: 0x666666 });

export class GroundBox extends Mesh<BoxBufferGeometry> {
    readonly body: Ammo.btRigidBody;

    constructor(width: number, height: number, position: { x: number; y: number; z: number }, rotationY?: number) {
        super(new BoxBufferGeometry(width, height, width), MATERIAL);
        this.position.set(position.x, position.y, position.z);
        if (rotationY != null) {
            this.rotateY(rotationY);
        }
        this.receiveShadow = true;
        this.castShadow = true;

        const transform = new Ammo.btTransform();
        transform.setOrigin(new Ammo.btVector3(position.x, position.y, position.z));
        if (rotationY) {
            const quaternion = new Ammo.btQuaternion(0, 0, 0, 0);
            quaternion.setEulerZYX(0, rotationY, 0);
            transform.setRotation(quaternion);
        }
        this.body = new Ammo.btRigidBody(
            new Ammo.btRigidBodyConstructionInfo(
                0,
                new Ammo.btDefaultMotionState(transform),
                new Ammo.btBoxShape(new Ammo.btVector3(width / 2, height / 2, width / 2))
            )
        );
    }
}
