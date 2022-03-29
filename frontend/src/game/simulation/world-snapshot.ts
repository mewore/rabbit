import { ArrayQueue, Queue } from '@/util/queue';

import { PlayerDisconnectMessage } from '../entities/messages/player-disconnect-message';
import { WorldUpdateMessage } from '../entities/messages/world-update-message';
import { PlayerInput } from '../entities/player/player-input';
import { PlayerState } from '../entities/player-state';

export class WorldSnapshot {
    playerStateById = new Map<number, PlayerState>();
    inputsByPlayerId = new Map<number, Queue<PlayerInput>>();

    constructor(private readonly framesToKeepInputs: number) {}

    applyUpdate(message: WorldUpdateMessage): void {
        for (const state of message.playerStates) {
            this.playerStateById.set(state.playerId, state);
        }

        const minInputFrame = message.frameId - this.framesToKeepInputs;
        for (const inputs of this.inputsByPlayerId.values()) {
            inputs.popWhile((input) => input.frameId < minInputFrame);
        }
    }

    applyUpdateInputs(message: WorldUpdateMessage): void {
        for (const playerInputs of message.newPlayerInputs.entries()) {
            const playerId = playerInputs[0];
            const newInputs = this.inputsByPlayerId.get(playerId) || new ArrayQueue();
            newInputs.pushAll(playerInputs[1]);
            this.inputsByPlayerId.set(playerInputs[0], newInputs);
        }
    }

    getLatestInputUntilFrame(playerId: number, frameId: number): PlayerInput | undefined {
        const inputs = this.inputsByPlayerId.get(playerId);
        if (!inputs) {
            return undefined;
        }
        let result: PlayerInput | undefined = undefined;
        for (const input of inputs) {
            if (input.frameId > frameId) {
                return result;
            }
            result = input;
        }
        return result;
    }

    applyDisconnect(message: PlayerDisconnectMessage): void {
        this.playerStateById.delete(message.playerId);
        this.inputsByPlayerId.delete(message.playerId);
    }
}
