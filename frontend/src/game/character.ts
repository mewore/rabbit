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
import { addCredit, isReisen } from '@/temp-util';
import { globalVector, makeAllCastAndReceiveShadow } from './three-util';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { Updatable } from './updatable';

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

const MAX_SPEED = 400.0;
const ACCELERATION = 1000.0;

const MOVEMENT_ANIMATION_THRESHOLDS: [number, number] = [1, 50];

const tmpVector3 = new Vector3();
const tmpVector2 = new Vector2();

export class Character extends Object3D implements Updatable {
    private readonly Y_OFFSET = 50.0;
    private animationInfo?: AnimationInfo;
    private currentMesh?: Object3D;

    private state = CharacterState.LOADING;

    private readonly targetMotion = new Vector3();
    private readonly motion = new Vector3();

    constructor() {
        super();
        this.name = 'Character';
        this.translateY(this.Y_OFFSET);

        const dummyBox = new Mesh(new BoxGeometry(100, 100, 100), new MeshBasicMaterial());
        dummyBox.name = 'CharacterDummyBox';
        dummyBox.receiveShadow = true;
        dummyBox.castShadow = true;
        dummyBox.position.set(10, 100.0, 50);
        this.mesh = dummyBox;

        if (isReisen()) {
            new GLTFLoader()
                .setPath('/assets/reisen/')
                .loadAsync('reisen.glb')
                .then((gltf) => {
                    const reisen = gltf.scene;
                    reisen.name = 'Reisen';
                    const reisenSize = 10;
                    reisen.scale.set(reisenSize, reisenSize, reisenSize);
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
                    const carrotSize = 50;
                    const carrot = gltf.scene;
                    carrot.name = 'Carrot';
                    carrot.scale.set(carrotSize, carrotSize, carrotSize);

                    makeAllCastAndReceiveShadow(carrot);
                    this.mesh = carrot;
                    const carrotUrl =
                        'https://sketchfab.com/3d-models/low-poly-carrot-31df366e091a4e64b9b0cfc1afc0145d';
                    const authorUrl = 'https://sketchfab.com/thepianomonster';
                    addCredit(
                        `<a href="${carrotUrl}" target="_blank">Carrot model</a> ` +
                            `by <a href="${authorUrl}" target="_blank">thepianomonster</a>`
                    );
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
        this.currentMesh.rotation.set(0, 0, 0);
        newMesh.translateY(-this.Y_OFFSET);
    }

    update(delta: number): void {
        const maxFrameAcceleration = ACCELERATION * delta;
        if (this.motion.distanceToSquared(this.targetMotion) < maxFrameAcceleration * maxFrameAcceleration) {
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

    stopMoving(): void {
        this.targetMotion.set(0, 0, 0);
    }

    move(viewpoint: Object3D, forward: number, right: number): void {
        const direction = globalVector(viewpoint, this);
        const angle = tmpVector2.set(direction.x, direction.z).angle() + tmpVector2.set(right, forward).angle();
        this.targetMotion.set(Math.cos(-angle) * MAX_SPEED, 0, Math.sin(-angle) * MAX_SPEED);
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
