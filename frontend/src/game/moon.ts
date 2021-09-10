import { DirectionalLight } from 'three';

export class Moon extends DirectionalLight {
    private static readonly DISTANCE = 200;

    constructor(shadowSharpnessRatio?: number) {
        super(0xdfebff, 1);

        this.name = 'Moon:' + shadowSharpnessRatio;
        this.position.set(1, 6, 4);
        this.position.normalize();
        this.position.multiplyScalar(Moon.DISTANCE);

        if (shadowSharpnessRatio != null) {
            this.castShadow = true;

            const shadowResolution = 8192;
            this.shadow.mapSize.width = this.shadow.mapSize.height = shadowResolution;

            const shadowSideMultiplier = 3;
            const shadowSideHalfLength = (shadowResolution * shadowSideMultiplier) / shadowSharpnessRatio / 2.0;
            this.shadow.camera.left = this.shadow.camera.bottom = -shadowSideHalfLength;
            this.shadow.camera.right = this.shadow.camera.top = shadowSideHalfLength;

            this.shadow.camera.far = Moon.DISTANCE * 5;
        }
    }
}