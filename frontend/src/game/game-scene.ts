import { Clock, Color, Fog, MOUSE, PerspectiveCamera, Scene, WebGLRenderer, sRGBEncoding } from 'three';
import { makeDefaultLights, makeGround } from './three-util';
import { Character } from './character';
import { OrbitControls } from '@three-ts/orbit-controls';
import { addCredit } from '../temp-util';

addCredit('<a target="_blank" href="https://threejs.org/">Three.js</a>');

export class GameScene {
    private readonly scene: THREE.Scene = new Scene();
    private readonly camera: THREE.PerspectiveCamera;
    private readonly character: Character = new Character();
    private readonly renderer: THREE.WebGLRenderer = new WebGLRenderer({ antialias: true });
    private readonly cameraControls: OrbitControls;

    private run = true;
    private requestedAnimationFrame?: number;

    private readonly clock = new Clock();

    constructor(wrapperElement: HTMLElement) {
        this.scene.background = new Color(0xcce0ff);
        this.scene.fog = new Fog(0xcce0ff, 500, 10000);

        this.camera = new PerspectiveCamera(100, window.innerWidth / window.innerHeight, 1, 10000);
        this.camera.position.set(100, 50, 200);

        this.scene.add(...makeDefaultLights());
        this.scene.add(makeGround());
        this.scene.add(this.character);

        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        wrapperElement.appendChild(this.renderer.domElement);
        this.renderer.outputEncoding = sRGBEncoding;
        this.renderer.shadowMap.enabled = true;

        this.cameraControls = new OrbitControls(this.camera, this.renderer.domElement);
        this.cameraControls.maxPolarAngle = Math.PI * 0.4;
        this.cameraControls.minDistance = 50;
        this.cameraControls.maxDistance = 1000;
        this.cameraControls.enablePan = false;
        this.cameraControls.enableDamping = true;
        this.cameraControls.dampingFactor = 0.1;
        this.cameraControls.mouseButtons = {
            ZOOM: MOUSE.MIDDLE,
            ORBIT: MOUSE.RIGHT,
            PAN: MOUSE.LEFT,
        };

        this.cameraControls.target = this.character.position;
        window.addEventListener('resize', this.onWindowResize.bind(this));
        this.animate();
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

    private animate() {
        if (!this.run) {
            return;
        }
        this.requestedAnimationFrame = requestAnimationFrame(this.animate.bind(this));
        const delta = this.clock.getDelta();

        this.character.update(delta);
        this.cameraControls.update();
        this.render();
    }

    private render() {
        this.renderer.render(this.scene, this.camera);
    }
}
