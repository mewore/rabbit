import { DirectionalLight, Vector3 } from 'three';

export class Sun extends DirectionalLight {
    private static readonly DISTANCE = 2000;

    constructor(shadowSharpnessRatio?: number) {
        super(0xdfebff, 1);

        this.name = 'Sun:' + shadowSharpnessRatio;
        this.position.set(1, 4, 2);
        this.position.normalize();
        this.position.multiplyScalar(Sun.DISTANCE);

        if (shadowSharpnessRatio != null) {
            this.castShadow = true;

            const shadowResolution = 8192;
            this.shadow.mapSize.width = this.shadow.mapSize.height = shadowResolution;

            const shadowSideMultiplier = 30;
            const shadowSideHalfLength = (shadowResolution * shadowSideMultiplier) / shadowSharpnessRatio / 2.0;
            this.shadow.camera.left = this.shadow.camera.bottom = -shadowSideHalfLength;
            this.shadow.camera.right = this.shadow.camera.top = shadowSideHalfLength;

            this.shadow.camera.far = 10000;
        }
    }

    static getSunDirection(): Vector3 {
        return new Vector3(1, 4, 2);
    }
}
