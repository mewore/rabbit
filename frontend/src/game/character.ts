import {
    AnimationAction,
    AnimationMixer,
    CylinderBufferGeometry,
    LoopOnce,
    Mesh,
    MeshBasicMaterial,
    Object3D,
    Vector2,
    Vector3,
} from 'three';
import { Body, Cylinder, Material, Ray, RayOptions, RaycastResult, Vec3, World } from 'cannon-es';
import { getAngleDifference, makeAllCastAndReceiveShadow, moveAngle } from './util/three-util';
import { PlayerState } from './entities/player-state';
import { Updatable } from './util/updatable';
import { Vector2Entity } from './entities/geometry/vector2-entity';
import { Vector3Entity } from './entities/geometry/vector3-entity';
import { Wrappable } from './util/wrappable';
import { addCredit } from '@/temp-util';
import { lerp } from 'three/src/math/MathUtils';
import { loadGltfWithCaching } from './util/gltf-util';

interface AnimationInfo {
    readonly mixer: AnimationMixer;
    readonly walkAction: AnimationAction;
    readonly runAction: AnimationAction;
    readonly idleAction: AnimationAction;
    readonly riseAction: AnimationAction;
    readonly fallAction: AnimationAction;
}

enum CharacterState {
    IDLE,
    WALKING,
    RUNNING,
    AIRBORNE,
}

const WALK_START_TIME = 0.2;
const RUN_START_TIME = 0.5;
const RUN_STOP_TIME = 0.3;
const WALK_STOP_TIME = 0.1;
const AIRBORNE_START_TIME = 0.3;
const AIRBORNE_STOP_TIME = 0.2;

const MIN_Y = 0;

const MAX_SPEED = 80;
const ACCELERATION = 200;
const JUMP_CONTROL_LENIENCY = 0.1;
const JUMP_SPEED = 100;
const MIN_Y_SPEED = -JUMP_SPEED * 2;

const MOVEMENT_ANIMATION_THRESHOLDS: [number, number] = [1, 20];

const tmpVector3 = new Vector3();
const horizontalMotion = new Vector2();
const tmpVector2 = new Vector2();

const TARGET_MOTION_CHANGE_THRESHOLD = 0.05;

const RADIUS = 3;
const HEIGHT = 10;
const MIN_ROTATION_SPEED = Math.PI * 0.1;
const MAX_ROTATION_SPEED = Math.PI * 3;

const MAX_ON_GROUND_VELOCITY = 1.0;
const GROUND_CHECK_PADDING = HEIGHT / 5;
const ray = new Ray();
const rayResult = new RaycastResult();
const groundRayOptions: RayOptions = { skipBackfaces: true };
const GROUND_CHECK_DX = [-RADIUS / 2, 0, RADIUS / 2];
const GROUND_CHECK_DZ = [-RADIUS / 2, 0, RADIUS / 2];

export class Character extends Object3D implements Updatable, Wrappable {
    readonly isWrappable = true;

    private animationInfo?: AnimationInfo;
    private currentMesh?: Object3D;

    private state = CharacterState.IDLE;

    private readonly targetHorizontalMotion = new Vector2();
    private jumpWantedAt = -Infinity;
    private hasChangedSinceLastQuery = true;

    private hasBeenSetUp = false;

    readonly body = new Body({
        fixedRotation: true,
        position: new Vec3(0, HEIGHT / 2, 0),
        mass: 1,
        material: new Material({ friction: 0, restitution: 0 }),
    });

    constructor(readonly username: string, isReisen: boolean | undefined, readonly offset = new Vector3()) {
        super();
        this.name = username ? 'Character:' + username : 'Character';

        this.body.addShape(new Cylinder(RADIUS, RADIUS, HEIGHT, 16), new Vec3(0, HEIGHT / 2, 0));
        const dummyBox = new Mesh(new CylinderBufferGeometry(RADIUS, RADIUS, HEIGHT, 16), new MeshBasicMaterial());
        dummyBox.name = 'CharacterDummyBox';
        dummyBox.receiveShadow = true;
        dummyBox.castShadow = true;
        dummyBox.position.set(0, HEIGHT / 2, 0);
        this.mesh = dummyBox;

        this.visible = false;
        if (isReisen != null) {
            this.setUpMesh(isReisen);
        }
    }

    setUpMesh(isReisen: boolean): void {
        if (this.hasBeenSetUp) {
            return;
        }
        this.hasBeenSetUp = true;
        this.visible = true;
        if (isReisen) {
            loadGltfWithCaching('/assets/reisen/reisen.glb').then((gltf) => {
                addCredit({
                    thing: { text: 'Fumo', url: 'https://fumo.website/' },
                    author: { text: 'ROYALCAT', url: 'https://royalcat.xyz/' },
                });

                const reisen = gltf.scene;
                reisen.name = 'Reisen';
                reisen.position.set(this.position.x, this.position.y, this.position.z);
                makeAllCastAndReceiveShadow(reisen);
                this.mesh = reisen;
                reisen.updateMatrix();
                reisen.rotation.y = 0;

                const mixer = new AnimationMixer(reisen);
                const idleAnimation = gltf.animations.find((animation) => animation.name === 'T-Pose');
                const walkAnimation = gltf.animations.find((animation) => animation.name === 'Walk');
                const runAnimation = gltf.animations.find((animation) => animation.name === 'Run');
                const riseAnimation = gltf.animations.find((animation) => animation.name === 'Rise');
                const fallAnimation = gltf.animations.find((animation) => animation.name === 'Fall');
                if (!idleAnimation || !walkAnimation || !runAnimation || !riseAnimation || !fallAnimation) {
                    return;
                }
                this.animationInfo = {
                    mixer,
                    walkAction: mixer.clipAction(walkAnimation),
                    runAction: mixer.clipAction(runAnimation),
                    idleAction: mixer.clipAction(idleAnimation),
                    riseAction: mixer.clipAction(riseAnimation),
                    fallAction: mixer.clipAction(fallAnimation),
                };
                this.animationInfo.idleAction.play();
            });
        } else {
            loadGltfWithCaching('/assets/carrot/scene.gltf').then((gltf) => {
                addCredit({
                    thing: {
                        text: 'Carrot model',
                        url: 'https://sketchfab.com/3d-models/low-poly-carrot-31df366e091a4e64b9b0cfc1afc0145d',
                    },
                    author: { text: 'thepianomonster', url: 'https://sketchfab.com/thepianomonster' },
                });
                const carrotSize = 5;
                const carrot = gltf.scene;
                carrot.name = 'Carrot';
                carrot.scale.set(carrotSize, carrotSize, carrotSize);

                makeAllCastAndReceiveShadow(carrot);
                this.mesh = carrot;
                this.animationInfo = undefined;
            });
        }
    }

    set mesh(newMesh: Object3D) {
        if (this.currentMesh) {
            this.currentMesh.removeFromParent();
        }
        this.attach(newMesh);
        this.currentMesh = newMesh;
        newMesh.rotation.set(0, 0, 0);
    }

    setState(newState: PlayerState): void {
        this.body.wakeUp();
        this.position.set(newState.position.x, newState.position.y, newState.position.z);
        this.body.position.set(newState.position.x, newState.position.y, newState.position.z);
        this.body.velocity.set(newState.motion.x, newState.motion.y, newState.motion.z);
        this.targetHorizontalMotion.set(newState.targetMotion.x, newState.targetMotion.y);
        this.hasChangedSinceLastQuery = true;
    }

    beforePhysics(delta: number, now: number): void {
        const maxFrameAcceleration = ACCELERATION * delta;
        horizontalMotion.set(this.body.velocity.x, this.body.velocity.z);
        const motionToTargetMotionDistanceSquared = horizontalMotion.distanceToSquared(this.targetHorizontalMotion);
        if (motionToTargetMotionDistanceSquared < maxFrameAcceleration * maxFrameAcceleration) {
            horizontalMotion.copy(this.targetHorizontalMotion);
        } else {
            horizontalMotion.add(
                tmpVector2.subVectors(this.targetHorizontalMotion, horizontalMotion).setLength(maxFrameAcceleration)
            );
        }

        this.body.velocity.x = horizontalMotion.x;
        this.body.velocity.z = horizontalMotion.y;

        if (this.isOnGround()) {
            this.body.velocity.y = 0;
            this.position.y = rayResult.hitPointWorld.y;
            if (now - this.jumpWantedAt < JUMP_CONTROL_LENIENCY) {
                this.jumpWantedAt = -Infinity;
                this.body.velocity.y = JUMP_SPEED;
                this.hasChangedSinceLastQuery = true;
            }
        }
        this.body.position.set(this.position.x, this.position.y, this.position.z);
    }

    afterPhysics(): void {
        if (this.body.position.y < MIN_Y) {
            this.body.position.y = MIN_Y;
            this.body.velocity.y = 0;
        } else if (this.body.velocity.y < MIN_Y_SPEED) {
            this.body.velocity.y = MIN_Y_SPEED;
        }
        this.position.set(this.body.position.x, this.body.position.y, this.body.position.z);
    }

    update(): void {}

    beforeRender(delta: number): void {
        const currentSpeedSquared = horizontalMotion.set(this.body.velocity.x, this.body.velocity.z).lengthSq();
        if (this.targetHorizontalMotion.lengthSq() > 0.0) {
            // The angles in Three.js are clockwise instead of counter-clockwise so the trigonometry is different
            const targetAngle = Math.atan2(this.targetHorizontalMotion.x, this.targetHorizontalMotion.y);
            const angleDifference = getAngleDifference(this.rotation.y, targetAngle);
            const rotationSpeed = lerp(MIN_ROTATION_SPEED, MAX_ROTATION_SPEED, angleDifference / Math.PI);
            this.rotation.y = moveAngle(this.rotation.y, targetAngle, rotationSpeed * delta);
        }
        if (!this.isOnGround()) {
            this.transitionIntoState(CharacterState.AIRBORNE);
            if (this.animationInfo) {
                const ySpeed = this.body.velocity.y;
                const riseCoefficient = ySpeed >= 0 ? ySpeed / JUMP_SPEED : -ySpeed / MIN_Y_SPEED;
                const riseActionWeight = 0.5 + riseCoefficient * 0.5;
                this.animationInfo.riseAction.setEffectiveWeight(riseActionWeight);
                this.animationInfo.fallAction.setEffectiveWeight(1 - riseActionWeight);
            }
        } else if (currentSpeedSquared < MOVEMENT_ANIMATION_THRESHOLDS[0] * MOVEMENT_ANIMATION_THRESHOLDS[0]) {
            this.transitionIntoState(CharacterState.IDLE);
        } else if (currentSpeedSquared < MOVEMENT_ANIMATION_THRESHOLDS[1] * MOVEMENT_ANIMATION_THRESHOLDS[1]) {
            this.transitionIntoState(CharacterState.WALKING);
        } else {
            this.transitionIntoState(CharacterState.RUNNING);
        }

        if (this.animationInfo) {
            this.animationInfo.mixer.update(delta);
        }
    }

    getHoverTextPosition(): Vector3 {
        return this.localToWorld(tmpVector3.set(0, 20, 0));
    }

    stopMoving(): void {
        this.setTargetMotion(0, 0);
    }

    requestMovement(viewpoint: Object3D, forward: number, right: number): void {
        const angle = viewpoint.rotation.y + tmpVector2.set(forward, right).angle();
        // The angles in Three.js are clockwise instead of counter-clockwise so the trigonometry is different
        this.setTargetMotion(Math.sin(angle) * MAX_SPEED, Math.cos(angle) * MAX_SPEED);
    }

    requestJump(now: number): void {
        this.body.wakeUp();
        this.jumpWantedAt = now;
    }

    private isOnGround(): boolean {
        if (!this.body.world || this.body.velocity.y > MAX_ON_GROUND_VELOCITY) {
            return false;
        }
        for (const dx of GROUND_CHECK_DX) {
            for (const dz of GROUND_CHECK_DZ) {
                if (this.tryGroundRay(dx, dz, this.body.world)) {
                    return true;
                }
            }
        }
        for (const offset of [-RADIUS, RADIUS]) {
            if (this.tryGroundRay(offset, 0, this.body.world) || this.tryGroundRay(0, offset, this.body.world)) {
                return true;
            }
        }
        return false;
    }

    private tryGroundRay(offsetX: number, offsetZ: number, world: World): boolean {
        this.localToWorld(tmpVector3.set(offsetX, 0, offsetZ));
        ray.from.set(tmpVector3.x, tmpVector3.y + GROUND_CHECK_PADDING, tmpVector3.z);
        ray.to.set(tmpVector3.x, tmpVector3.y - GROUND_CHECK_PADDING, tmpVector3.z);
        world.raycastAny(ray.from, ray.to, groundRayOptions, rayResult);
        return rayResult.hasHit;
    }

    private setTargetMotion(x: number, z: number): void {
        this.body.wakeUp();
        if (
            tmpVector2.set(x, z).sub(this.targetHorizontalMotion).lengthSq() / (MAX_SPEED * MAX_SPEED) >
            TARGET_MOTION_CHANGE_THRESHOLD
        ) {
            this.targetHorizontalMotion.set(x, z);
            this.hasChangedSinceLastQuery = true;
        }
    }

    hasChanged(): boolean {
        const result = this.hasChangedSinceLastQuery;
        this.hasChangedSinceLastQuery = false;
        return result;
    }

    getState(): PlayerState {
        return new PlayerState(
            Vector3Entity.fromVector3(this.position),
            Vector3Entity.fromVector3(this.body.velocity),
            Vector2Entity.fromVector2(this.targetHorizontalMotion)
        );
    }

    /**
     * @param newState The new state to transition into.
     * @param duration The duration of the transition in SECONDS.
     */
    private transitionIntoState(newState: CharacterState): void {
        if (this.state === newState) {
            return;
        }
        if (this.animationInfo && newState !== this.state) {
            const duration = this.getActionDuration(this.state, newState);
            const oldActions = this.getActionsForState(this.state, this.animationInfo);
            const newActions = this.getActionsForState(newState, this.animationInfo);
            for (const oldAction of oldActions) {
                oldAction.fadeOut(duration);
            }

            for (const newAction of newActions) {
                newAction.reset().setEffectiveTimeScale(1).setEffectiveWeight(1).fadeIn(duration);
                if (newAction.loop !== LoopOnce && oldActions.length === 1) {
                    newAction.startAt(oldActions[0].time);
                }
                newAction.play();
            }
        }
        this.state = newState;
    }

    private getActionsForState(state: CharacterState, animationInfo: AnimationInfo): AnimationAction[] {
        switch (state) {
            case CharacterState.IDLE:
                return [animationInfo.idleAction];
            case CharacterState.WALKING:
                return [animationInfo.walkAction];
            case CharacterState.RUNNING:
                return [animationInfo.runAction];
            case CharacterState.AIRBORNE:
                return [animationInfo.riseAction, animationInfo.fallAction];
        }
    }

    private getActionDuration(sourceState: CharacterState, targetState: CharacterState): number {
        if (targetState === CharacterState.AIRBORNE) {
            return AIRBORNE_START_TIME;
        }
        switch (sourceState) {
            case CharacterState.IDLE:
                switch (targetState) {
                    case CharacterState.WALKING:
                        return WALK_START_TIME;
                    case CharacterState.RUNNING:
                        return RUN_START_TIME;
                }
                return 0;
            case CharacterState.RUNNING:
                switch (targetState) {
                    case CharacterState.IDLE:
                    case CharacterState.WALKING:
                        return RUN_STOP_TIME;
                }
                return 0;
            case CharacterState.WALKING:
                switch (targetState) {
                    case CharacterState.IDLE:
                        return WALK_STOP_TIME;
                    case CharacterState.RUNNING:
                        return RUN_START_TIME;
                }
                return 0;
            case CharacterState.AIRBORNE:
                return AIRBORNE_STOP_TIME;
        }
    }
}
