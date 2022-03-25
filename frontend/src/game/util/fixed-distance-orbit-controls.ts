import Ammo from 'ammo.js';
import { Object3D, Raycaster, Spherical, Vector3 } from 'three';

import { BulletCollisionMasks } from '../physics/bullet-collision-masks';
import { Input } from './input';
import { RenderAware } from './render-aware';

const normal = new Vector3();
const rayDirection = new Vector3();
const newTargetWorldPosition = new Vector3();
const raycaster = new Raycaster();

const EPSILON = 0.000001;
const MIN_PHI = EPSILON;
const MAX_PHI = Math.PI - EPSILON;

let tmpRayFrom: Ammo.btVector3;
let tmpRayTo: Ammo.btVector3;

export class FixedDistanceOrbitControls implements RenderAware {
    private static lastId = 0;

    private readonly spherical = new Spherical();

    intersectionObjects: Object3D[] = [];

    zoomMultiplier = 1.2;

    readonly id = 'FixedDistanceOrbitControls:' + ++FixedDistanceOrbitControls.lastId;

    /**
     * From 0 to 1, with 0 meaning it never reaches its target Y position and 1 meaning it immediately teleports there.
     *
     * Every T seconds, the distance between the apparent target position and its real position is multiplied by
     * [(1 - yPositionChangeSpeed) ^ T].
     */
    yPositionChangeSpeed = 0.95;

    padding = 10;

    offset = new Vector3();

    minDistance = 0.0;
    maxDistance = Infinity;

    private readonly targetWorldPosition = new Vector3();

    private readonly raycastResultCallback = new Ammo.ClosestRayResultCallback(
        new Ammo.btVector3(),
        new Ammo.btVector3()
    );

    constructor(
        private readonly input: Input,
        private readonly object: Object3D,
        private readonly target: Object3D,
        private readonly physicsWorld: Ammo.btCollisionWorld
    ) {
        target.getWorldPosition(this.targetWorldPosition);
        this.spherical.setFromVector3(object.getWorldPosition(new Vector3()).sub(this.targetWorldPosition));
        object.lookAt(this.targetWorldPosition);
        this.raycastResultCallback.set_m_collisionFilterMask(BulletCollisionMasks.STATIC_OBJECTS);
    }

    longBeforeRender(delta: number): void {
        if (this.input.zoom) {
            this.spherical.radius = Math.min(
                Math.max(this.spherical.radius / Math.pow(this.zoomMultiplier, this.input.zoom), this.minDistance),
                this.maxDistance
            );
        }

        this.spherical.theta -= this.input.lookRight;
        this.spherical.phi = Math.max(MIN_PHI, Math.min(MAX_PHI, this.spherical.phi - this.input.lookDown));

        this.object.position.setFromSpherical(this.spherical);
        this.object.lookAt(0, 0, 0);
        this.target.getWorldPosition(newTargetWorldPosition);
        newTargetWorldPosition.add(this.offset);

        const targetYPositionDistance = Math.pow(1 - this.yPositionChangeSpeed, delta);
        this.targetWorldPosition.set(
            newTargetWorldPosition.x,
            this.targetWorldPosition.y * targetYPositionDistance +
                newTargetWorldPosition.y * (1.0 - targetYPositionDistance),
            newTargetWorldPosition.z
        );

        if (!this.tryToIntersect()) {
            this.object.position.add(this.targetWorldPosition);
        }
    }

    beforeRender(): void {}

    /**
     * Check for intersections from the target to the wanted object position. If there are any,
     * the object is moved in front of the first such intersection so that there is nothing between it and its target.
     * @returns Whether there are any intersections.
     */
    private tryToIntersect(): boolean {
        const rayDistance = this.spherical.radius + this.padding;
        rayDirection.copy(this.object.position).normalize();
        let minHitDistance = Infinity;
        let rayHit: Ammo.btVector3 | Vector3 | undefined = undefined;

        // Ammo.js raycast
        const rayFrom = (tmpRayFrom = tmpRayFrom || new Ammo.btVector3());
        const rayTo = (tmpRayTo = tmpRayTo || new Ammo.btVector3());

        rayFrom.setValue(this.targetWorldPosition.x, this.targetWorldPosition.y, this.targetWorldPosition.z);
        rayTo.setValue(
            this.targetWorldPosition.x + rayDirection.x * rayDistance,
            this.targetWorldPosition.y + rayDirection.y * rayDistance,
            this.targetWorldPosition.z + rayDirection.z * rayDistance
        );

        this.raycastResultCallback.set_m_closestHitFraction(Infinity);
        this.physicsWorld.rayTest(rayFrom, rayTo, this.raycastResultCallback);
        if (this.raycastResultCallback.hasHit()) {
            minHitDistance = this.raycastResultCallback.get_m_closestHitFraction() * rayDistance;
            const newNormal = this.raycastResultCallback.get_m_hitNormalWorld();
            normal.set(newNormal.x(), newNormal.y(), newNormal.z());
            // For some reason, get_m_hitPointWorld is not the actual point, so it has to be adjusted
            rayHit = this.raycastResultCallback.get_m_hitPointWorld();
            rayHit.setValue(
                this.targetWorldPosition.x + rayDirection.x * minHitDistance,
                this.targetWorldPosition.y + rayDirection.y * minHitDistance,
                this.targetWorldPosition.z + rayDirection.z * minHitDistance
            );
        }

        // Three.js raycast
        if (this.intersectionObjects.length) {
            raycaster.set(this.targetWorldPosition, rayDirection);
            raycaster.far = rayDistance;
            const intersection = raycaster.intersectObjects(this.intersectionObjects, true)[0];
            if (intersection && intersection.distance < minHitDistance && intersection.face) {
                normal.copy(intersection.face.normal).applyMatrix4(intersection.object.matrixWorld).normalize();
                minHitDistance = intersection.distance;
                rayHit = intersection.point;
            }
        }

        // The more parallel the camera is to the face that has been hit, more it should be pulled back
        const distanceOffset = -this.padding * (1 - Math.abs(normal.dot(rayDirection)));
        if (minHitDistance + distanceOffset > this.spherical.radius || !rayHit) {
            return false;
        }

        if (rayHit instanceof Vector3) {
            this.object.position.copy(rayHit);
        } else {
            this.object.position.set(rayHit.x(), rayHit.y(), rayHit.z());
        }
        this.object.position.addScaledVector(rayDirection, distanceOffset);

        return true;
    }
}
