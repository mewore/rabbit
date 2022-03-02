import { Object3D, Vector3 } from 'three';

import { Updatable } from './updatable';

export class AutoFollow implements Updatable {
    static lastId = 0;

    private readonly offset: Vector3;

    readonly id = 'AutoFollow:' + ++AutoFollow.lastId;

    constructor(private readonly object: Object3D, private readonly target: Object3D) {
        this.offset = new Vector3(object.position.x, object.position.y, object.position.z);
    }

    beforePhysics(): void {}

    afterPhysics(): void {}

    update(): void {
        this.object.position.copy(this.target.position);
        this.object.position.add(this.offset);
        this.object.updateMatrix();
    }

    beforeRender(): void {}
}
