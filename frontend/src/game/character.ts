import { AnimationMixer, BoxGeometry, Clock, Mesh, MeshBasicMaterial, Object3D } from 'three';
import { addCredit, isReisen } from '@/temp-util';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { makeAllCastShadow } from './three-util';

interface AnimationInfo {
    readonly mixer: AnimationMixer;
    clock: Clock;
    readonly stepDuration: number;
}

export class Character extends Object3D {
    private readonly Y_OFFSET = 50.0;
    private animationInfo?: AnimationInfo;

    constructor() {
        super();
        this.name = 'Character';
        this.translateY(this.Y_OFFSET);

        const dummyBox = new Mesh(new BoxGeometry(100, 100, 100), new MeshBasicMaterial());
        dummyBox.name = 'CharacterDummyBox';
        dummyBox.receiveShadow = true;
        dummyBox.castShadow = true;
        dummyBox.position.set(10, 100.0, 50);
        this.mesh = dummyBox;

        if (isReisen()) {
            new GLTFLoader()
                .setPath('/assets/reisen/')
                .loadAsync('scene.gltf')
                .then((gltf) => {
                    const reisen = gltf.scene;
                    reisen.name = 'Reisen';
                    const reisenSize = 10;
                    reisen.scale.set(reisenSize, reisenSize, reisenSize);
                    reisen.position.set(this.position.x, this.position.y, this.position.z);
                    makeAllCastShadow(reisen);
                    this.mesh = reisen;

                    const mixer = new AnimationMixer(reisen);
                    const walkAnimation = gltf.animations[0];
                    mixer.clipAction(walkAnimation).play();
                    this.animationInfo = {
                        clock: new Clock(),
                        mixer,
                        stepDuration: walkAnimation.duration / 2.0,
                    };
                });
        } else {
            new GLTFLoader()
                .setPath('/assets/carrot/')
                .loadAsync('scene.gltf')
                .then((gltf) => {
                    const carrotSize = 50;
                    const carrot = gltf.scene;
                    carrot.name = 'Carrot';
                    carrot.scale.set(carrotSize, carrotSize, carrotSize);

                    makeAllCastShadow(carrot);
                    this.mesh = carrot;
                    const carrotUrl =
                        'https://sketchfab.com/3d-models/low-poly-carrot-31df366e091a4e64b9b0cfc1afc0145d';
                    const authorUrl = 'https://sketchfab.com/thepianomonster';
                    addCredit(
                        `<a href="${carrotUrl}" target="_blank">Carrot model</a> ` +
                            `by <a href="${authorUrl}" target="_blank">thepianomonster</a>`
                    );
                });
        }
    }

    set mesh(newMesh: Object3D) {
        this.clear();
        this.attach(newMesh);
        newMesh.translateY(-this.Y_OFFSET);
    }

    update(delta: number): void {
        this.rotation.y += 1 * delta;

        const movementSpeed =
            this.animationInfo == null
                ? 1.0
                : 1.0 - this.square((this.animationInfo.clock.getElapsedTime() / this.animationInfo.stepDuration) % 1);

        this.translateZ(movementSpeed * 100.0 * delta);

        if (this.animationInfo) {
            this.animationInfo.mixer.update(delta);
        }
    }

    private square(num: number): number {
        return num * num;
    }
}
