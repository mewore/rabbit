import {
    AnimationAction,
    AnimationMixer,
    BoxGeometry,
    Clock,
    LoopOnce,
    Mesh,
    MeshBasicMaterial,
    Object3D,
} from 'three';
import { addCredit, isReisen } from '@/temp-util';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { Updatable } from './updatable';
import { makeAllCastAndReceiveShadow } from './three-util';

interface AnimationInfo {
    readonly mixer: AnimationMixer;
    readonly walkAction: AnimationAction;
    readonly runAction: AnimationAction;
}

enum CharacterState {
    LOADING,
    IDLE,
    WALKING,
    RUNNING,
}

export class Character extends Object3D implements Updatable {
    private readonly Y_OFFSET = 50.0;
    private animationInfo?: AnimationInfo;
    private currentMesh?: Object3D;

    private state = CharacterState.LOADING;
    private previousState = CharacterState.LOADING;
    private transitionClock?: Clock;
    private transitionDuration = 0.0;

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

                    const mixer = new AnimationMixer(reisen);
                    const walkAnimation = gltf.animations.find((animation) => animation.name === 'Walk');
                    const runAnimation = gltf.animations.find((animation) => animation.name === 'Run');
                    if (!walkAnimation || !runAnimation) {
                        return;
                    }
                    const walkAction = mixer.clipAction(walkAnimation);
                    const runAction = mixer.clipAction(runAnimation);
                    walkAction.play();
                    this.animationInfo = {
                        mixer,
                        walkAction,
                        runAction,
                    };
                    this.state = CharacterState.WALKING;
                    setInterval(() => {
                        if (this.state === CharacterState.WALKING) {
                            this.transitionIntoState(CharacterState.RUNNING, 1.0);
                        } else if (this.state === CharacterState.RUNNING) {
                            this.transitionIntoState(CharacterState.WALKING, 1.0);
                        }
                    }, 7000);
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
        newMesh.translateY(-this.Y_OFFSET);
    }

    update(delta: number): void {
        const movementSpeed = this.getMovementSpeed();

        if (movementSpeed > 0.0) {
            this.rotation.y += (0.5 * movementSpeed * delta) / 300.0;
            this.translateZ(movementSpeed * delta);
        }

        if (this.animationInfo) {
            this.animationInfo.mixer.update(delta);
        }
    }

    private getMovementSpeed(): number {
        const transitionRatio = this.getTransitionRatio();
        return (
            this.getSpeedForState(this.state) * transitionRatio +
            this.getSpeedForState(this.previousState) * (1.0 - transitionRatio)
        );
    }

    private getSpeedForState(state: CharacterState): number {
        switch (state) {
            case CharacterState.RUNNING:
                return 300.0;
            case CharacterState.WALKING:
                return 50.0;
            default:
                return 0.0;
        }
    }

    private getTransitionRatio(): number {
        if (!this.transitionClock) {
            return 1.0;
        }
        const ratio = this.transitionClock.getElapsedTime() / this.transitionDuration;
        if (isNaN(ratio) || ratio > 1.0) {
            this.transitionClock = undefined;
            return 1.0;
        }
        return ratio;
    }

    /**
     * @param newState The new state to transition into.
     * @param duration The duration of the transition in SECONDS.
     */
    private transitionIntoState(newState: CharacterState, duration: number): void {
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
        this.transitionClock = new Clock();
        this.transitionDuration = duration;
        this.previousState = this.state;
        this.state = newState;
    }

    private getActionForState(state: CharacterState): AnimationAction | undefined {
        switch (state) {
            case CharacterState.RUNNING:
                return this.animationInfo?.runAction;
            case CharacterState.WALKING:
                return this.animationInfo?.walkAction;
            default:
                return undefined;
        }
    }
}
