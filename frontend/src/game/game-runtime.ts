import { AnimationMixer, Clock } from 'three';
import { addCredit, isReisen } from '../temp-util';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';
import { OrbitControls } from '@three-ts/orbit-controls';
import { makeAllCastShadow } from './three-util';

export class GameRuntime {
    private character: THREE.Object3D;

    private run = true;
    private requestedAnimationFrame?: number;

    private readonly clock = new Clock();
    private animationMixer?: THREE.AnimationMixer;

    constructor(
        private readonly camera: THREE.PerspectiveCamera,
        private readonly scene: THREE.Scene,
        private readonly renderer: THREE.WebGLRenderer,
        private readonly controls: OrbitControls,

        dummyBox: THREE.Object3D
    ) {
        this.character = dummyBox;
        if (isReisen()) {
            new GLTFLoader().setPath('/assets/reisen/').load('scene.gltf', (gltf) => {
                const reisen = gltf.scene;
                const reisenSize = 10;
                reisen.scale.set(reisenSize, reisenSize, reisenSize);
                reisen.position.set(dummyBox.position.x, dummyBox.position.y, dummyBox.position.z);
                makeAllCastShadow(reisen);
                scene.add(reisen);
                scene.remove(dummyBox);

                const mixer = new AnimationMixer(reisen);
                mixer.clipAction(gltf.animations[0]).play();
                this.animationMixer = mixer;

                this.character = reisen;
            });
        } else {
            new GLTFLoader().setPath('/assets/carrot/').load('scene.gltf', (gltf) => {
                const carrotSize = 50;
                const carrot = gltf.scene;
                carrot.scale.set(carrotSize, carrotSize, carrotSize);

                makeAllCastShadow(carrot);
                scene.add(carrot);
                scene.remove(dummyBox);
                this.character = carrot;
                const carrotUrl = 'https://sketchfab.com/3d-models/low-poly-carrot-31df366e091a4e64b9b0cfc1afc0145d';
                const authorUrl = 'https://sketchfab.com/thepianomonster';
                addCredit(
                    `<a href="${carrotUrl}" target="_blank">Carrot model</a> ` +
                        `by <a href="${authorUrl}" target="_blank">thepianomonster</a>`
                );
            });
        }
        window.addEventListener('resize', this.onWindowResize.bind(this));
        this.animate(0);
    }

    stopRunning(): void {
        this.run = false;
        if (typeof this.requestedAnimationFrame === 'number') {
            cancelAnimationFrame(this.requestedAnimationFrame);
        }
    }

    private onWindowResize() {
        this.camera.aspect = window.innerWidth / window.innerHeight;
        this.camera.updateProjectionMatrix();

        this.renderer.setSize(window.innerWidth, window.innerHeight);

        this.render();
    }

    private animate(now: number) {
        if (!this.run) {
            return;
        }
        this.requestedAnimationFrame = requestAnimationFrame(this.animate.bind(this));
        const delta = this.clock.getDelta();

        this.controls.update();
        this.character.rotation.y = now * 0.001;

        if (this.animationMixer) {
            this.animationMixer.update(delta);
        }
        this.render();
    }

    private render() {
        this.renderer.render(this.scene, this.camera);
    }
}
