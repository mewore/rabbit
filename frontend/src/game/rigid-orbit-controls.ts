import { Object3D, Raycaster, Spherical, Vector3 } from 'three';
import { Input } from './input';
import { Updatable } from './updatable';

const tmpVector3 = new Vector3();
const otherTmpVector3 = new Vector3();

const EPSILON = 0.000001;
const MIN_PHI = EPSILON;
const MAX_PHI = Math.PI - EPSILON;

export class RigidOrbitControls implements Updatable {
    speedRetention = 0.000001;
    private phiDelta = 0.0;
    private thetaDelta = 0.0;
    private readonly spherical = new Spherical();

    intersectionObjects?: Object3D[];

    zoomMultiplier = 1.2;

    constructor(private readonly input: Input, private readonly object: Object3D, private readonly target: Object3D) {
        const targetWorldPosition = target.getWorldPosition(new Vector3());
        this.spherical.setFromVector3(object.getWorldPosition(new Vector3()).sub(targetWorldPosition));
        object.lookAt(targetWorldPosition);
    }

    update(delta: number): void {
        this.thetaDelta -= this.input.lookRight;
        this.phiDelta -= this.input.lookDown;

        if (this.input.zoom) {
            this.spherical.radius /= Math.pow(this.zoomMultiplier, this.input.zoom);
        }

        this.input.clearMouseDelta();

        this.spherical.theta += this.thetaDelta;
        this.spherical.phi = Math.max(MIN_PHI, Math.min(MAX_PHI, this.spherical.phi + this.phiDelta));

        if (this.speedRetention) {
            const currentSpeedRetention = Math.pow(this.speedRetention, delta);
            this.thetaDelta *= currentSpeedRetention;
            this.phiDelta *= currentSpeedRetention;
        } else {
            this.thetaDelta = this.phiDelta = 0;
        }

        this.object.position.setFromSpherical(this.spherical);
        this.object.lookAt(0, 0, 0);
        const targetWorldPosition = this.target.getWorldPosition(tmpVector3);

        if (this.intersectionObjects && this.intersectionObjects.length) {
            const direction = otherTmpVector3.copy(this.object.position).normalize();
            const raycaster = new Raycaster(targetWorldPosition, direction, 0, this.spherical.radius);
            const intersections = raycaster.intersectObjects(this.intersectionObjects, true);
            if (intersections.length) {
                this.object.position.copy(intersections[0].point);
            } else {
                this.object.position.add(targetWorldPosition);
            }
        } else {
            this.object.position.add(targetWorldPosition);
        }
    }
}
