import {
    AnimationAction,
    AnimationMixer,
    BoxBufferGeometry,
    LoopOnce,
    Mesh,
    MeshBasicMaterial,
    Object3D,
    Vector2,
    Vector3,
} from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { PlayerState } from './entities/player-state';
import { Updatable } from './updatable';
import { Vector2Entity } from './entities/vector2-entity';
import { Vector3Entity } from './entities/vector3-entity';
import { addCredit } from '@/temp-util';
import { makeAllCastAndReceiveShadow } from './three-util';

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
const RUN_START_TIME = 1.0;
const RUN_STOP_TIME = 0.3;
const WALK_STOP_TIME = 0.1;
const AIRBORNE_START_TIME = 0.3;
const AIRBORNE_STOP_TIME = 0.2;

const Y_OFFSET = 7.5;
const MIN_Y = Y_OFFSET;

const MAX_SPEED = 40;
const ACCELERATION = 100;
const JUMP_CONTROL_LENIENCY = 0.1;
const JUMP_SPEED = 80;
const MIN_Y_SPEED = -JUMP_SPEED * 2;
const GRAVITY = 200;

const MOVEMENT_ANIMATION_THRESHOLDS: [number, number] = [1, 5];

const tmpVector3 = new Vector3();
const tmpVector2 = new Vector2();

const TARGET_MOTION_CHANGE_THRESHOLD = 0.05;

export class Character extends Object3D implements Updatable {
    private animationInfo?: AnimationInfo;
    private currentMesh?: Object3D;

    private state = CharacterState.IDLE;

    private readonly targetHorizontalMotion = new Vector2();
    private readonly horizontalMotion = new Vector2();
    private jumpWantedAt = -Infinity;
    private ySpeed = 0;
    private hasChangedSinceLastQuery = true;

    private hasBeenSetUp = false;

    constructor(readonly username: string, isReisen: boolean | undefined) {
        super();
        this.name = username ? 'Character:' + username : 'Character';
        this.translateY(Y_OFFSET);

        const dummyBox = new Mesh(new BoxBufferGeometry(10, 10, 10), new MeshBasicMaterial());
        dummyBox.name = 'CharacterDummyBox';
        dummyBox.receiveShadow = true;
        dummyBox.castShadow = true;
        dummyBox.position.set(1, 10, 5);
        this.mesh = dummyBox;

        this.visible = false;
        if (isReisen != null) {
            this.setUpMesh(isReisen);
        }
    }

    public setUpMesh(isReisen: boolean): void {
        if (this.hasBeenSetUp) {
            return;
        }
        this.hasBeenSetUp = true;
        this.visible = true;
        if (isReisen) {
            new GLTFLoader()
                .setPath('/assets/reisen/')
                .loadAsync('reisen.glb')
                .then((gltf) => {
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
            new GLTFLoader()
                .setPath('/assets/carrot/')
                .loadAsync('scene.gltf')
                .then((gltf) => {
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
        newMesh.position.set(0, 0, 0);
        newMesh.translateY(-Y_OFFSET);
    }

    setState(newState: PlayerState): void {
        this.position.set(newState.position.x, newState.position.y, newState.position.z);
        this.horizontalMotion.set(newState.motion.x, newState.motion.z);
        this.ySpeed = newState.motion.y;
        this.targetHorizontalMotion.set(newState.targetMotion.x, newState.targetMotion.y);
        this.hasChangedSinceLastQuery = true;
    }

    update(delta: number, now: number): void {
        const maxFrameAcceleration = ACCELERATION * delta;
        const motionToTargetMotionDistanceSquared = this.horizontalMotion.distanceToSquared(
            this.targetHorizontalMotion
        );
        if (motionToTargetMotionDistanceSquared < maxFrameAcceleration * maxFrameAcceleration) {
            this.horizontalMotion.copy(this.targetHorizontalMotion);
        } else {
            this.horizontalMotion.add(
                tmpVector2
                    .subVectors(this.targetHorizontalMotion, this.horizontalMotion)
                    .setLength(maxFrameAcceleration)
            );
        }

        if (this.horizontalMotion.lengthSq() > 0.0) {
            this.rotation.y = this.horizontalMotion.angle();
            this.translateZ(this.horizontalMotion.length() * delta);
        }

        if (this.position.y > MIN_Y) {
            this.ySpeed -= GRAVITY * delta;
            if (this.ySpeed < MIN_Y_SPEED) {
                this.ySpeed = MIN_Y_SPEED;
            }
        } else if (now - this.jumpWantedAt < JUMP_CONTROL_LENIENCY) {
            this.jumpWantedAt = -Infinity;
            this.ySpeed = JUMP_SPEED;
            this.hasChangedSinceLastQuery = true;
        }
        this.position.y += this.ySpeed * delta;
        if (this.position.y < MIN_Y) {
            this.position.y = MIN_Y;
            this.ySpeed = 0;
        }

        const currentSpeedSquared = this.horizontalMotion.lengthSq();
        if (this.position.y > MIN_Y) {
            this.transitionIntoState(CharacterState.AIRBORNE);
            if (this.animationInfo) {
                const riseCoefficient = this.ySpeed >= 0 ? this.ySpeed / JUMP_SPEED : -this.ySpeed / MIN_Y_SPEED;
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

    move(viewpoint: Object3D, forward: number, right: number): void {
        const angle = viewpoint.rotation.y + tmpVector2.set(forward, right).angle();
        this.setTargetMotion(Math.cos(angle) * MAX_SPEED, Math.sin(angle) * MAX_SPEED);
    }

    jump(now: number): void {
        this.jumpWantedAt = now;
    }

    private setTargetMotion(x: number, z: number): void {
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
            new Vector3Entity(this.horizontalMotion.x, this.ySpeed, this.horizontalMotion.y),
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
