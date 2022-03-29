import Ammo from 'ammo.js';

import { Character } from '@/game/character';
import { PhysicsAware } from '@/game/util/physics-aware';
import { ArrayQueue, Queue } from '@/util/queue';

import { FrameAnalysis } from '../debug/frame-analysis';
import { WorldUpdateState } from '../debug/frame-info';
import { WorldUpdateMessage } from '../entities/messages/world-update-message';
import { PlayerInput } from '../entities/player/player-input';
import { MazeMap } from '../entities/world/maze-map';
import { ServerClock } from './server-clock';
import { WorldSnapshot } from './world-snapshot';

const FPS = 60;
const SECONDS_PER_FRAME = 1.0 / FPS;
const MILLISECONDS_TO_SECONDS = 1 / 1000;
const MAX_CONSECUTIVE_SIMULATED_FRAMES = 2 * FPS;
const RESEND_LAST_INPUT_SECONDS = 0.5;
const RESEND_LAST_INPUT_LATENCY_MULTIPLIER = 2;

export class WorldSimulation {
    /**
     * All inputs entered by the player since the last world update received from the server.
     */
    private readonly inputEventsSinceReceivedState: Queue<PlayerInput> = new ArrayQueue<PlayerInput>();
    private pendingInputs: Queue<PlayerInput> = new ArrayQueue<PlayerInput>();

    private readonly latestSnapshot = new WorldSnapshot(FPS * 10);

    private readonly serverClock = new ServerClock();

    private readonly spheres: Ammo.btRigidBody[] = [];

    private readonly tmpVector3 = new Ammo.btVector3();

    private _currentFrame = -1;

    get currentFrame(): number {
        return this._currentFrame;
    }

    get hasServerUpdate(): boolean {
        return this._currentFrame > -1;
    }

    map?: MazeMap;

    shouldResendInput = false;

    lastAppliedUpdateFrame = -1;

    constructor(
        private readonly frameAnalysis: FrameAnalysis,
        private readonly physicsWorld: Ammo.btDiscreteDynamicsWorld,
        private readonly physicsAwareById: Map<number | string, PhysicsAware>,
        private readonly selfCharacter: Character,
        private readonly characterById: Map<number, Character>,
        private readonly sphereCreator: () => Ammo.btRigidBody
    ) {}

    acceptInput(input: PlayerInput) {
        this.inputEventsSinceReceivedState.push(input);
        this.pendingInputs.push(input);
        this.latestSnapshot.applyUpdateInputs;
    }

    applyUpdateInputs(message: WorldUpdateMessage): void {
        this.latestSnapshot.applyUpdateInputs(message);
    }

    applyUpdate(message: WorldUpdateMessage, now: number): void {
        const newSelfState = message.playerStates.find((state) => state.playerId === this.selfCharacter.playerId);
        this.latestSnapshot.applyUpdate(message);

        if (this.frameAnalysis.analyzing) {
            this.frameAnalysis.pendingWorldUpdateState ||= WorldUpdateState.ACCEPTED;
        }

        if (newSelfState) {
            const newInput = this.latestSnapshot.getLatestInputUntilFrame(newSelfState.playerId, message.frameId);
            const newInputId = newInput ? newInput.id : -1;

            // Remove all inputs which the server has already applied to the state it has sent back to the client
            this.inputEventsSinceReceivedState.popWhile((input) => newInput != null && newInput.id >= input.id);
            this.serverClock.guessServerTime(
                now,
                message.frameId * SECONDS_PER_FRAME,
                newSelfState.latency * MILLISECONDS_TO_SECONDS
            );

            const oldestInputNotSimulatedByServer = this.inputEventsSinceReceivedState.front;
            if (oldestInputNotSimulatedByServer && message.frameId > oldestInputNotSimulatedByServer.frameId) {
                if (this.frameAnalysis.analyzing) {
                    this.frameAnalysis.pendingWorldUpdateState = WorldUpdateState.REJECTED;
                    this.frameAnalysis.addMessage(
                        `Cannot roll back from world update for frame #${message.frameId} ` +
                            `because it's at input @${newInputId} but is more recent ` +
                            `than input @${oldestInputNotSimulatedByServer.id}, ` +
                            `which was sent at frame #${oldestInputNotSimulatedByServer.frameId}`
                    );
                }
                const allowedFrameDifference = Math.round(
                    (RESEND_LAST_INPUT_SECONDS +
                        RESEND_LAST_INPUT_LATENCY_MULTIPLIER * newSelfState.latency * MILLISECONDS_TO_SECONDS) /
                        SECONDS_PER_FRAME
                );
                if (message.frameId - oldestInputNotSimulatedByServer.frameId >= allowedFrameDifference) {
                    // The server not knowing about an input sent by the client should never happen because
                    // WebSocket is more reliable than UDP but if it ever does, it should be fixed as soon as possible.
                    // Otherwise, states of the client and the server will continue diverging until there's a new input.
                    this.shouldResendInput = true;
                    if (process.env.NODE_ENV === 'development') {
                        window.console.warn(
                            `World update at frame #${message.frameId} and input #${newInputId} ` +
                                'is much more recent than an input not acknowledged by the server yet ' +
                                `(input #${oldestInputNotSimulatedByServer.id} ` +
                                `at frame #${oldestInputNotSimulatedByServer.frameId}). Resending the current input.`
                        );
                    }
                }
                return;
            }

            this.pendingInputs = (
                this.latestSnapshot.inputsByPlayerId.get(this.selfCharacter.playerId)?.clone() || new ArrayQueue()
            ).pushAll(this.inputEventsSinceReceivedState);

            this.selfCharacter.clearInput();
            this.selfCharacter.applyNewState(newSelfState);
            this._currentFrame = message.frameId;
        }

        if (this.frameAnalysis.analyzing) {
            this.frameAnalysis.addMessage({
                text: `Accepted world update for frame #${message.frameId}`,
                attachments: [
                    { icon: 'public', reference: message, tooltip: 'New world state' },
                    {
                        icon: 'keyboard',
                        reference: new Map<number, PlayerInput[]>(
                            Array.from(this.latestSnapshot.inputsByPlayerId).map((entry) => [
                                entry[0],
                                Array.from(entry[1]),
                            ])
                        ),
                        tooltip: 'Inputs so far',
                    },
                ],
            });
            if (newSelfState) {
                this.frameAnalysis.addMessage('Player info for new world update:');
                this.frameAnalysis.addMessage('\t - Position: ', newSelfState.controllerState.position);
                this.frameAnalysis.addMessage('\t - Velocity: ', newSelfState.controllerState.motion);
                this.frameAnalysis.addMessage('\t - at input #', this.selfCharacter.lastAppliedInputId);
                this.frameAnalysis.addMessage('\t - Target V: ', this.selfCharacter.controller.targetHorizontalMotion);
                this.frameAnalysis.addMessage(
                    `\tPending inputs: ${Array.from(this.pendingInputs)
                        .map((input) => `[#${input.id} at frame #${input.frameId}: ${input}]`)
                        .join('; ')}`
                );
            }
        }

        for (const newState of message.playerStates) {
            const character = this.characterById.get(newState.playerId);
            const input = this.latestSnapshot.getLatestInputUntilFrame(newState.playerId, message.frameId);
            if (character && input) {
                character.applyInput(input);
            }
        }

        let sphereIndex = 0;
        for (const sphereUpdate of message.spheres) {
            if (sphereIndex >= this.spheres.length) {
                this.spheres.push(this.sphereCreator());
            }
            const sphere = this.spheres[sphereIndex++];
            sphere.forceActivationState(sphereUpdate.activationState);
            sphere
                .getWorldTransform()
                .getOrigin()
                .setValue(sphereUpdate.position.x, sphereUpdate.position.y, sphereUpdate.position.z);
            this.tmpVector3.setValue(sphereUpdate.motion.x, sphereUpdate.motion.y, sphereUpdate.motion.z);
            sphere.setLinearVelocity(this.tmpVector3);
        }
    }

    private doStep(delta: number) {
        if (this.frameAnalysis.analyzing) {
            this.frameAnalysis.addMessage(`Frame #${this._currentFrame} -> #${this._currentFrame + 1}`);
            this.frameAnalysis.addMessage('Player state:');
            this.frameAnalysis.addMessage('\t - Position: ', this.selfCharacter.controller.position);
            this.frameAnalysis.addMessage('\t - Velocity: ', this.selfCharacter.controller.motion);
            this.frameAnalysis.addMessage('\t - at input #', this.selfCharacter.lastAppliedInputId);
            this.frameAnalysis.addMessage('\t - Target V: ', this.selfCharacter.controller.targetHorizontalMotion);
        }
        this.wrapEverything();
        if (this.frameAnalysis.analyzing) {
            this.frameAnalysis.addMessage('After wrapping:');
            this.frameAnalysis.addMessage('\t - Position: ', this.selfCharacter.controller.position);
        }
        for (const input of this.pendingInputs.popWhile((input) => this._currentFrame >= input.frameId)) {
            if (this.frameAnalysis.analyzing) {
                this.frameAnalysis.addMessage(
                    `Applying input for frame #${this._currentFrame + 1}: #${input.id} | ${input}`
                );
                this.frameAnalysis.addMessage('Player state:');
                this.frameAnalysis.addMessage('\t - at input #', this.selfCharacter.lastAppliedInputId);
                this.frameAnalysis.addMessage('\t - Target V: ', this.selfCharacter.controller.targetHorizontalMotion);
            }
            this.selfCharacter.applyInput(input);
        }

        for (const updatable of this.physicsAwareById.values()) {
            updatable.beforePhysics(delta, this._currentFrame * SECONDS_PER_FRAME);
        }
        this.physicsWorld.stepSimulation(delta, 0, delta);
        ++this._currentFrame;
        for (const updatable of this.physicsAwareById.values()) {
            updatable.afterPhysics(delta, this._currentFrame * SECONDS_PER_FRAME);
        }
        this.wrapEverything();
    }

    private wrapEverything(): void {
        if (!this.map) {
            return;
        }
        this.map.wrapPosition(this.selfCharacter.position);
        this.selfCharacter.setBodyPosition(this.selfCharacter.position);
        for (const character of this.characterById.values()) {
            if (character.playerId !== this.selfCharacter.playerId) {
                this.map.wrapTowards(character.position, this.selfCharacter.position);
                character.setBodyPosition(character.position);
            }
        }
    }

    simulateUntil(targetTimestamp: number): void {
        if (this._currentFrame === -1) {
            return;
        }
        const targetFrame = Math.round(this.serverClock.localTimeToServerTime(targetTimestamp) / SECONDS_PER_FRAME) - 1;
        const framesToSimulate = Math.max(
            0,
            Math.min(MAX_CONSECUTIVE_SIMULATED_FRAMES, targetFrame - this.currentFrame)
        );
        if (framesToSimulate > 20 && this.frameAnalysis.analyzing) {
            this.frameAnalysis.addMessage(`Simulating from frame #${this._currentFrame} to frame ${targetFrame}`);
        }
        for (let i = 0; i < framesToSimulate; i++) {
            this.doStep(SECONDS_PER_FRAME);
        }
    }
}
