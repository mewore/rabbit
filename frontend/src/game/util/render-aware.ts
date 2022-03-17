export interface RenderAware {
    readonly id: string | number;

    longBeforeRender(delta: number, now: number): void;

    beforeRender(delta: number, now: number): void;
}
