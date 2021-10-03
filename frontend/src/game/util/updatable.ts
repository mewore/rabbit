export interface Updatable {
    readonly id: string | number;

    beforePhysics(delta: number, now: number): void;
    afterPhysics(delta: number, now: number): void;
    update(delta: number, now: number): void;
    beforeRender(delta: number, now: number): void;
}
