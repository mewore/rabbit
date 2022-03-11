export interface RenderAware {
    readonly id: string | number;

    beforeRender(delta: number, now: number): void;
}
