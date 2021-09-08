import {
    AnimationAction,
    AnimationMixer,
    BoxGeometry,
    LoopOnce,
    Mesh,
    MeshBasicMaterial,
    Object3D,
    Vector2,
    Vector3,
} from 'three';
import { globalVector, makeAllCastAndReceiveShadow } from './three-util';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { PlayerState } from './entities/player-state';
import { Updatable } from './updatable';
import { Vector3Entity } from './entities/vector3-entity';
import { addCredit } from '@/temp-util';

interface AnimationInfo {
    readonly mixer: AnimationMixer;
    readonly walkAction: AnimationAction;
    readonly runAction: AnimationAction;
    readonly idleAction: AnimationAction;
}

enum CharacterState {
    LOADING,
    IDLE,
    WALKING,
    RUNNING,
}

const WALK_START_TIME = 0.2;
const RUN_START_TIME = 1.0;
const RUN_STOP_TIME = 0.3;
const WALK_STOP_TIME = 0.1;

const MAX_SPEED = 40;
const ACCELERATION = 100;

const MOVEMENT_ANIMATION_THRESHOLDS: [number, number] = [1, 5];

const tmpVector3 = new Vector3();
const tmpVector2 = new Vector2();

const TARGET_MOTION_CHANGE_THRESHOLD = 0.05;

export class Character extends Object3D implements Updatable {
    private readonly Y_OFFSET = 7.5;
    private animationInfo?: AnimationInfo;
    private currentMesh?: Object3D;

    private state = CharacterState.LOADING;

    private readonly targetMotion = new Vector3();
    private readonly motion = new Vector3();
    private hasChangedSinceLastQuery = true;

    private hasBeenSetUp = false;

    constructor(readonly username: string, isReisen: boolean | undefined) {
        super();
        this.name = username ? 'Character:' + username : 'Character';
        this.translateY(this.Y_OFFSET);

        const dummyBox = new Mesh(new BoxGeometry(10, 10, 10), new MeshBasicMaterial());
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
                    if (!idleAnimation || !walkAnimation || !runAnimation) {
                        return;
                    }
                    this.animationInfo = {
                        mixer,
                        walkAction: mixer.clipAction(walkAnimation),
                        runAction: mixer.clipAction(runAnimation),
                        idleAction: mixer.clipAction(idleAnimation),
                    };
                    this.animationInfo.idleAction.play();
                    this.state = CharacterState.IDLE;
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
                    this.state = CharacterState.IDLE;
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
        newMesh.translateY(-this.Y_OFFSET);
    }

    setState(newState: PlayerState): void {
        this.position.set(newState.position.x, newState.position.y, newState.position.z);
        this.motion.set(newState.motion.x, newState.motion.y, newState.motion.z);
        this.targetMotion.set(newState.targetMotion.x, newState.targetMotion.y, newState.targetMotion.z);
        this.hasChangedSinceLastQuery = true;
    }

    update(delta: number): void {
        const maxFrameAcceleration = ACCELERATION * delta;
        const motionToTargetMotionDistanceSquared = this.motion.distanceToSquared(this.targetMotion);
        if (motionToTargetMotionDistanceSquared < maxFrameAcceleration * maxFrameAcceleration) {
            this.motion.copy(this.targetMotion);
        } else {
            this.motion.add(tmpVector3.subVectors(this.targetMotion, this.motion).setLength(maxFrameAcceleration));
        }

        if (this.motion.lengthSq() > 0.0) {
            this.rotation.y = tmpVector2.set(this.motion.x, this.motion.z).angle();
            this.translateZ(this.motion.length() * delta);
        }

        const currentSpeedSquared = this.motion.lengthSq();
        if (currentSpeedSquared < MOVEMENT_ANIMATION_THRESHOLDS[0] * MOVEMENT_ANIMATION_THRESHOLDS[0]) {
            this.transitionIntoState(
                CharacterState.IDLE,
                this.state === CharacterState.WALKING ? WALK_STOP_TIME : RUN_STOP_TIME
            );
        } else if (currentSpeedSquared < MOVEMENT_ANIMATION_THRESHOLDS[1] * MOVEMENT_ANIMATION_THRESHOLDS[1]) {
            this.transitionIntoState(
                CharacterState.WALKING,
                this.state === CharacterState.IDLE ? WALK_START_TIME : RUN_STOP_TIME
            );
        } else {
            this.transitionIntoState(CharacterState.RUNNING, RUN_START_TIME);
        }

        if (this.animationInfo) {
            this.animationInfo.mixer.update(delta);
        }
    }

    getHoverTextPosition(): Vector3 {
        return this.localToWorld(tmpVector3.set(0, 20, 0));
    }

    stopMoving(): void {
        this.setTargetMotion(0, 0, 0);
    }

    move(viewpoint: Object3D, forward: number, right: number): void {
        const direction = globalVector(viewpoint, this);
        const angle = tmpVector2.set(direction.x, direction.z).angle() + tmpVector2.set(right, forward).angle();
        this.setTargetMotion(Math.cos(-angle) * MAX_SPEED, 0, Math.sin(-angle) * MAX_SPEED);
    }

    private setTargetMotion(x: number, y: number, z: number): void {
        if (
            tmpVector3.set(x, y, z).sub(this.targetMotion).lengthSq() / (MAX_SPEED * MAX_SPEED) >
            TARGET_MOTION_CHANGE_THRESHOLD
        ) {
            this.targetMotion.set(x, y, z);
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
            Vector3Entity.fromVector3(this.motion),
            Vector3Entity.fromVector3(this.targetMotion)
        );
    }

    /**
     * @param newState The new state to transition into.
     * @param duration The duration of the transition in SECONDS.
     */
    private transitionIntoState(newState: CharacterState, duration: number): void {
        if (this.state === newState) {
            return;
        }
        if (this.animationInfo && newState !== this.state) {
            const oldAction = this.getActionForState(this.state);
            const newAction = this.getActionForState(newState);
            if (oldAction) {
                oldAction.fadeOut(duration);
            }
            if (newAction) {
                newAction.reset().setEffectiveTimeScale(1).setEffectiveWeight(1).fadeIn(duration);
                if (newAction.loop !== LoopOnce && oldAction) {
                    newAction.startAt(oldAction.time);
                }
                newAction.play();
            }
        }
        this.state = newState;
    }

    private getActionForState(state: CharacterState): AnimationAction | undefined {
        switch (state) {
            case CharacterState.IDLE:
                return this.animationInfo?.idleAction;
            case CharacterState.WALKING:
                return this.animationInfo?.walkAction;
            case CharacterState.RUNNING:
                return this.animationInfo?.runAction;
            default:
                return undefined;
        }
    }
}
