export interface Updatable {
    update(delta: number, now: number): void;
}
