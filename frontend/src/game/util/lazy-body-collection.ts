import { Body, Vec3, World } from 'cannon-es';
import { Updatable } from './updatable';
import { Vector3 } from 'three';

export class LazyBodyCollection implements Updatable {
    private static _instanceIndex = 0;
    readonly id = `StaticBodyCollection:${LazyBodyCollection._instanceIndex++}`;

    padding = 500;
    activeBodyCount = 0;

    constructor(
        private readonly world: World,
        readonly bodies: ReadonlyArray<Body>,
        readonly referencePosition: Vector3 | Vec3
    ) {}

    beforePhysics(): void {
        this.activeBodyCount = 0;
        for (const body of this.bodies) {
            if (
                body.aabb.lowerBound.x - this.padding < this.referencePosition.x &&
                body.aabb.lowerBound.z - this.padding < this.referencePosition.z &&
                body.aabb.upperBound.x + this.padding > this.referencePosition.x &&
                body.aabb.upperBound.z + this.padding > this.referencePosition.z
            ) {
                this.activeBodyCount++;
                this.world.addBody(body);
            } else {
                this.world.removeBody(body);
            }
        }
    }

    afterPhysics(): void {}

    update(): void {}

    beforeRender(): void {}
}
