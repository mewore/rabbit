import Ammo from 'ammo.js';

import { Character } from '@/game/character';
import { PhysicsAware } from '@/game/util/physics-aware';
import { ArrayQueue, Queue } from '@/util/queue';

import { FrameAnalysis } from '../debug/frame-analysis';
import { WorldUpdateMessage } from '../entities/messages/world-update-message';
import { PlayerInputMutation } from '../entities/mutations/player-input-mutation';
import { MazeMap } from '../entities/world/maze-map';
import { ServerClock } from './server-clock';
import { WorldSnapshot } from './world-snapshot';

const FPS = 60;
const SECONDS_PER_FRAME = 1.0 / FPS;
const MILLISECONDS_TO_SECONDS = 1 / 1000;
const MAX_CONSECUTIVE_SIMULATED_FRAMES = 2 * FPS;
const RESEND_LAST_INPUT_FRAMES = FPS;

export class WorldSimulation {
    /**
     * All inputs entered by the player since the last world update received from the server.
     */
    private readonly inputEventsSinceReceivedState: Queue<PlayerInputMutation> = new ArrayQueue<PlayerInputMutation>();
    private pendingInputEvents: Queue<PlayerInputMutation> = new ArrayQueue<PlayerInputMutation>();

    private readonly latestSnapshot = new WorldSnapshot();

    private readonly serverClock = new ServerClock();

    private readonly spheres: Ammo.btRigidBody[] = [];

    private _currentFrame = -1;

    get currentFrame(): number {
        return this._currentFrame;
    }

    get hasServerUpdate(): boolean {
        return this._currentFrame > -1;
    }

    private lastAcknowledgedInput?: PlayerInputMutation;

    map?: MazeMap;

    shouldResendInput = false;

    constructor(
        private readonly physicsWorld: Ammo.btDiscreteDynamicsWorld,
        private readonly physicsAwareById: Map<number | string, PhysicsAware>,
        private readonly selfCharacter: Character,
        private readonly characterById: Map<number, Character>,
        private readonly sphereCreator: () => Ammo.btRigidBody
    ) {}

    acceptInput(input: PlayerInputMutation) {
        this.inputEventsSinceReceivedState.push(input);
        this.pendingInputEvents.push(input);
    }

    applyUpdate(message: WorldUpdateMessage, now: number): void {
        const newSelfState = message.playerStates.find((state) => state.playerId === this.selfCharacter.playerId);
        this.latestSnapshot.applyUpdate(message);

        if (newSelfState) {
            // Remove all inputs which the server has already applied to the state it has sent back to the client
            this.inputEventsSinceReceivedState.popWhile((input) => {
                if (newSelfState.input.id >= input.id) {
                    this.lastAcknowledgedInput = input;
                    return true;
                }
                return false;
            });
            this.serverClock.guessServerTime(
                now,
                message.frameId * SECONDS_PER_FRAME,
                newSelfState.latency * MILLISECONDS_TO_SECONDS
            );

            const oldestInputNotSimulatedByServer = this.inputEventsSinceReceivedState.front;
            if (oldestInputNotSimulatedByServer && message.frameId > oldestInputNotSimulatedByServer.frameId) {
                if (message.frameId - oldestInputNotSimulatedByServer.frameId >= RESEND_LAST_INPUT_FRAMES) {
                    // The server not knowing about an input sent by the client should never happen because
                    // WebSocket is more reliable than UDP but if it ever does, it should be fixed as soon as possible.
                    // Otherwise, states of the client and the server will continue diverging until there's a new input.
                    this.shouldResendInput = true;
                    if (process.env.NODE_ENV === 'development') {
                        window.console.warn(
                            `World update at frame #${message.frameId} and input #${newSelfState.input.id} ` +
                                'is much more recent than an input not acknowledged by the server yet ' +
                                `(input #${oldestInputNotSimulatedByServer.id} ` +
                                `at frame #${oldestInputNotSimulatedByServer.frameId}). Resending the current input.`
                        );
                    }
                }
                return;
            }

            this.pendingInputEvents = this.inputEventsSinceReceivedState.clone();

            if (process.env.NODE_ENV === 'development' && FrameAnalysis.GLOBAL.analyzing) {
                FrameAnalysis.GLOBAL.addMessage(`Received update; player position: ${newSelfState.position}`);
            }
            if (this.lastAcknowledgedInput) {
                this.selfCharacter.applyInput(this.lastAcknowledgedInput);
            }
            this.selfCharacter.applyNewState(newSelfState);
            this._currentFrame = message.frameId;
        }

        let sphereIndex = 0;
        for (const spherePosition of message.spherePositions) {
            if (sphereIndex >= this.spheres.length) {
                this.spheres.push(this.sphereCreator());
            }
            this.spheres[sphereIndex++]
                .getWorldTransform()
                .getOrigin()
                .setValue(spherePosition.x, spherePosition.y, spherePosition.z);
        }
    }

    private doStep(delta: number) {
        for (const inputEvent of this.pendingInputEvents.popWhile((input) => this._currentFrame >= input.frameId)) {
            this.selfCharacter.applyInput(inputEvent);
        }

        this.wrapEverything();
        if (this.map) {
            for (const character of this.characterById.values()) {
                character.wrapStateToCurrentPosition(this.map);
            }
        }

        if (process.env.NODE_ENV === 'development' && FrameAnalysis.GLOBAL.analyzing) {
            FrameAnalysis.GLOBAL.addMessage(`<Frame #${this.currentFrame} -> #${this.currentFrame + 1}>`);
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
        for (let i = 0; i < framesToSimulate; i++) {
            this.doStep(SECONDS_PER_FRAME);
        }
    }
}
