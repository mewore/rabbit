import { PlayerControllerState } from './player/player-controller-state';

export class PlayerState {
    constructor(readonly playerId: number, readonly latency: number, readonly controllerState: PlayerControllerState) {}
}
