import { Vector3 } from 'three';

export interface Wrappable {
    readonly isWrappable: true;
    readonly position: Vector3;
    readonly offset: Vector3;
}
