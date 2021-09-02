import { Object3D, Vector3 } from 'three';
import { Updatable } from './updatable';

export class AutoFollow implements Updatable {
    private readonly offset: Vector3;

    constructor(private readonly object: Object3D, private readonly target: Object3D) {
        this.offset = new Vector3(object.position.x, object.position.y, object.position.z);
    }

    update(): void {
        this.object.position.copy(this.target.position);
        this.object.position.add(this.offset);
        this.object.updateMatrix();
    }
}
