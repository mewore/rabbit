import { Object3D, Raycaster, Spherical, Vector3 } from 'three';
import { Input } from './input';
import { Updatable } from './updatable';

const rayDirection = new Vector3();
const newTargetWorldPosition = new Vector3();
const raycaster = new Raycaster();

const EPSILON = 0.000001;
const MIN_PHI = EPSILON;
const MAX_PHI = Math.PI - EPSILON;

export class FixedDistanceOrbitControls implements Updatable {
    private readonly spherical = new Spherical();

    intersectionObjects?: Object3D[];

    zoomMultiplier = 1.2;

    /**
     * From 0 to 1, with 0 meaning it never reaches its target Y position and 1 meaning it immediately teleports there.
     *
     * Every T seconds, the distance between the apparent target position and its real position is multiplied by
     * [(1 - yPositionChangeSpeed) ^ T].
     */
    yPositionChangeSpeed = 0.95;

    padding = 5;

    private readonly targetWorldPosition = new Vector3();

    constructor(private readonly input: Input, private readonly object: Object3D, private readonly target: Object3D) {
        target.getWorldPosition(this.targetWorldPosition);
        this.spherical.setFromVector3(object.getWorldPosition(new Vector3()).sub(this.targetWorldPosition));
        object.lookAt(this.targetWorldPosition);
    }

    update(delta: number): void {
        if (this.input.zoom) {
            this.spherical.radius /= Math.pow(this.zoomMultiplier, this.input.zoom);
        }

        this.spherical.theta -= this.input.lookRight;
        this.spherical.phi = Math.max(MIN_PHI, Math.min(MAX_PHI, this.spherical.phi - this.input.lookDown));
        this.input.clearMouseDelta();

        this.object.position.setFromSpherical(this.spherical);
        this.object.lookAt(0, 0, 0);
        this.target.getWorldPosition(newTargetWorldPosition);

        const targetYPositionDistance = Math.pow(1 - this.yPositionChangeSpeed, delta);
        this.targetWorldPosition.set(
            newTargetWorldPosition.x,
            this.targetWorldPosition.y * targetYPositionDistance +
                newTargetWorldPosition.y * (1.0 - targetYPositionDistance),
            newTargetWorldPosition.z
        );

        if (this.intersectionObjects && this.intersectionObjects.length && this.spherical.radius > -this.padding) {
            rayDirection.copy(this.object.position).normalize();
            raycaster.set(this.targetWorldPosition, rayDirection);
            raycaster.far = this.spherical.radius + this.padding;
            const intersection = raycaster.intersectObjects(this.intersectionObjects, true)[0];
            if (intersection) {
                this.object.position.copy(intersection.point).addScaledVector(rayDirection, -this.padding);
            } else {
                this.object.position.add(this.targetWorldPosition);
            }
        } else {
            this.object.position.add(this.targetWorldPosition);
        }
    }
}