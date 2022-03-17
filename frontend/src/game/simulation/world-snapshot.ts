import { PlayerDisconnectMessage } from '../entities/messages/player-disconnect-message';
import { WorldUpdateMessage } from '../entities/messages/world-update-message';
import { PlayerState } from '../entities/player-state';

export class WorldSnapshot {
    playerStateById = new Map<number, PlayerState>();

    applyUpdate(message: WorldUpdateMessage): void {
        for (const state of message.playerStates) {
            this.playerStateById.set(state.playerId, state);
        }
    }

    applyDisconnect(message: PlayerDisconnectMessage): void {
        this.playerStateById.delete(message.playerId);
    }
}
