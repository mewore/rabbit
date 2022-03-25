import Ammo from 'ammo.js';
import { btCollisionWorld } from 'ammo.js';

const ACCELERATION = 400;

export const JUMP_SPEED = 110;

export const MAX_Y_SPEED = JUMP_SPEED * 2;

const GROUND_CHECK_REQUIRED_NORMAL_Y = 0.3;

type Vector3 = { readonly x: number; readonly y: number };

let tmpVector3: Ammo.btVector3;

export class RigidBodyController {
    groundLeniency = 0.1;
    jumpControlLeniency = 0.1;

    targetHorizontalMotion: Vector3 = { x: 0, y: 0 };

    groundTimeLeft = 0;
    jumpControlTimeLeft = 0;

    constructor(private readonly body: Ammo.btRigidBody) {}

    get position(): Ammo.btVector3 {
        return this.body.getWorldTransform().getOrigin();
    }

    get motion(): Ammo.btVector3 {
        return this.body.getLinearVelocity();
    }

    jump(): void {
        this.jumpControlTimeLeft = this.jumpControlLeniency;
    }

    onGround(): boolean {
        return this.groundTimeLeft >= 0;
    }

    private wantsToJump(): boolean {
        return this.jumpControlTimeLeft >= 0;
    }

    updateAction(_world: Ammo.btCollisionWorld, deltaTimeStep: number): void {
        const motion = this.body.getLinearVelocity();
        const motionDx = this.targetHorizontalMotion.x - motion.x();
        const motionDz = this.targetHorizontalMotion.y - motion.z();
        const motionDistanceSquared = motionDx * motionDx + motionDz * motionDz;
        const maxFrameAcceleration = ACCELERATION * deltaTimeStep;
        if (motionDistanceSquared > maxFrameAcceleration * maxFrameAcceleration) {
            const multiplier = maxFrameAcceleration / Math.sqrt(motionDistanceSquared);
            motion.setX(motion.x() + motionDx * multiplier);
            motion.setZ(motion.z() + motionDz * multiplier);
        } else {
            motion.setX(this.targetHorizontalMotion.x);
            motion.setZ(this.targetHorizontalMotion.y);
        }
        if (this.onGround() && this.wantsToJump()) {
            motion.setY(JUMP_SPEED);
            this.groundTimeLeft = this.jumpControlTimeLeft = 0;
        } else {
            motion.setY(Math.max(-MAX_Y_SPEED, Math.min(MAX_Y_SPEED, motion.y())));
        }

        this.body.setLinearVelocity(motion);

        if (Math.abs(motion.x()) > 0 || Math.abs(motion.y()) > 0 || Math.abs(motion.z()) > 0) {
            this.body.activate();
        }

        this.jumpControlTimeLeft -= deltaTimeStep;
        this.groundTimeLeft -= deltaTimeStep;
    }

    afterPhysics(world: Ammo.btCollisionWorld): void {
        this.forEachGroundCollision(world, () => (this.groundTimeLeft = this.groundLeniency));
    }

    private forEachGroundCollision(world: btCollisionWorld, callback: (normal: Ammo.btVector3) => void): void {
        const dispatcher = world.getDispatcher();
        for (let i = 0; i < dispatcher.getNumManifolds(); i++) {
            const manifold = dispatcher.getManifoldByIndexInternal(i);
            const isFirstBody = Ammo.castObject(manifold.getBody0(), Ammo.btRigidBody) === this.body;
            const isSecondBody = !isFirstBody && Ammo.castObject(manifold.getBody1(), Ammo.btRigidBody) === this.body;
            if (isFirstBody || isSecondBody) {
                for (let j = 0; j < manifold.getNumContacts(); j++) {
                    const normal = manifold.getContactPoint(j).get_m_normalWorldOnB();
                    if ((isFirstBody ? 1 - normal.y() : 1 + normal.y()) < GROUND_CHECK_REQUIRED_NORMAL_Y) {
                        if (isFirstBody) {
                            callback(normal);
                        } else {
                            const fixedNormal = (tmpVector3 = tmpVector3 || new Ammo.btVector3());
                            fixedNormal.setValue(-normal.x(), -normal.y(), -normal.z());
                            callback(fixedNormal);
                        }
                    }
                }
            }
        }
    }
}
