export interface PhysicsAware {
    readonly id: string | number;

    beforePhysics(delta: number, now: number): void;
    afterPhysics(delta: number, now: number): void;
}
