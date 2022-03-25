import Ammo from 'ammo.js';
import { AdditiveBlending, BoxBufferGeometry, Mesh, MeshPhongMaterial } from 'three';

import { BulletCollisionFlags } from '../physics/bullet-collision-flags';

const MATERIAL = new MeshPhongMaterial({ color: 0x666666 });

export class PhysicsDummyBox extends Mesh<BoxBufferGeometry> {
    static SHINY_MATERIAL = new MeshPhongMaterial({
        color: MATERIAL.color,
        emissive: 0xffff66,
        emissiveIntensity: 0.5,
        transparent: true,
        opacity: 0.5,
        blending: AdditiveBlending,
    });

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
        this.body.setCollisionFlags(BulletCollisionFlags.STATIC_OBJECT);
        this.body.setFriction(0.25);
        this.body.setRestitution(0.4);
    }
}
