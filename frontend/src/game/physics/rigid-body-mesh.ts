import { Object3D } from 'three/src/Three';

import { RenderAware } from '../util/render-aware';

export class RigidBodyFollow implements RenderAware {
    private static lastId = 0;

    readonly id = 'RigidBodyFollow:' + ++RigidBodyFollow.lastId;

    private readonly objects: Object3D[];

    constructor(readonly body: Ammo.btRigidBody, ...objects: Object3D[]) {
        this.objects = [objects].flat();
    }

    longBeforeRender(): void {}

    beforeRender(): void {
        const newPos = this.body.getWorldTransform().getOrigin();
        for (const object of this.objects) {
            object.position.set(newPos.x(), newPos.y(), newPos.z());
        }
    }
}
