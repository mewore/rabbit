import Ammo from 'ammo.js';
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
import { lerp } from 'three/src/math/MathUtils';

import { addCredit } from '@/temp-util';

import { PlayerInput } from './entities/player/player-input';
import { PlayerState } from './entities/player-state';
import { JUMP_SPEED, MAX_Y_SPEED, RigidBodyController } from './physics/rigid-body-controller';
import { loadGltfWithCaching } from './util/gltf-util';
import { PhysicsAware } from './util/physics-aware';
import { RenderAware } from './util/render-aware';
import { getAngleDifference, makeAllCastAndReceiveShadow, moveAngle } from './util/three-util';

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

const MAX_SPEED = 100;

const MOVEMENT_ANIMATION_THRESHOLDS: [number, number] = [1, 20];

const tmpVector3 = new Vector3();
const horizontalMotion = new Vector2();

const RADIUS = 3;
const HEIGHT = 10;
const MIN_ROTATION_SPEED = Math.PI * 0.1;
const MAX_ROTATION_SPEED = Math.PI * 3;

let playerShape: Ammo.btConvexShape;

export class Character extends Object3D implements PhysicsAware, RenderAware {
    private animationInfo?: AnimationInfo;
    private currentMesh?: Object3D;

    lastAppliedInputId = -1;

    playerId = -1;

    private state = CharacterState.IDLE;

    private readonly targetHorizontalMotion = new Vector2();

    private wantsToJump = false;

    private hasBeenSetUp = false;

    public inputId = -1;

    readonly body: Ammo.btRigidBody;
    readonly controller: RigidBodyController;

    constructor(
        private readonly world: Ammo.btDiscreteDynamicsWorld,
        public username: string,
        isReisen: boolean | undefined,
        readonly isSelf = false
    ) {
        super();
        this.name = username ? 'Character:' + username : 'Character';

        const shape = (playerShape =
            playerShape || new Ammo.btCylinderShape(new Ammo.btVector3(RADIUS, HEIGHT / 2, RADIUS)));
        this.body = new Ammo.btRigidBody(
            new Ammo.btRigidBodyConstructionInfo(1, new Ammo.btDefaultMotionState(), shape)
        );
        this.body.setFriction(0);
        this.body.setRestitution(0);
        this.body
            .getWorldTransform()
            .getOrigin()
            .setY(HEIGHT / 2);

        this.controller = new RigidBodyController(this.body);
        this.controller.targetHorizontalMotion = this.targetHorizontalMotion;

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

    applyNewState(newState: PlayerState): void {
        this.controller.applyNewState(newState.controllerState);
        newState.controllerState.position.paste(this.position);
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
        newMesh.position.set(0, -HEIGHT / 2, 0);
        newMesh.rotation.set(0, 0, 0);
    }

    setBodyPosition(position: { readonly x: number; readonly y: number; readonly z: number }): void {
        this.controller.setPosition(position);
    }

    beforePhysics(delta: number): void {
        if (this.wantsToJump) {
            this.controller.jump();
        }
        this.controller.updateAction(this.world, delta);
    }

    afterPhysics(): void {
        this.controller.afterPhysics(this.world);
        const newPosition = this.controller.position;
        this.position.set(newPosition.x(), newPosition.y(), newPosition.z());
    }

    longBeforeRender(): void {}

    beforeRender(delta: number): void {
        const motion = this.controller.motion;
        const currentSpeedSquared = horizontalMotion.set(motion.x(), motion.z()).lengthSq();
        if (this.targetHorizontalMotion.lengthSq() > 0.0) {
            // The angles in Three.js are clockwise instead of counter-clockwise so the trigonometry is different
            const targetAngle = Math.atan2(this.targetHorizontalMotion.x, this.targetHorizontalMotion.y);
            const angleDifference = getAngleDifference(this.rotation.y, targetAngle);
            const rotationSpeed = lerp(MIN_ROTATION_SPEED, MAX_ROTATION_SPEED, angleDifference / Math.PI);
            this.rotation.y = moveAngle(this.rotation.y, targetAngle, rotationSpeed * delta);
        }
        if (!this.controller.onGround()) {
            this.transitionIntoState(CharacterState.AIRBORNE);
            if (this.animationInfo) {
                const ySpeed = motion.y();
                const riseCoefficient = ySpeed >= 0 ? ySpeed / JUMP_SPEED : -ySpeed / MAX_Y_SPEED;
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

    applyInput(input: PlayerInput): void {
        this.lastAppliedInputId = input.id;
        input.applyToTargetMotion(this.targetHorizontalMotion);
        this.targetHorizontalMotion.multiplyScalar(MAX_SPEED);
        this.wantsToJump = input.wantsToJump;
        this.inputId = input.id;
    }

    clearInput(): void {
        this.applyInput(PlayerInput.EMPTY);
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
