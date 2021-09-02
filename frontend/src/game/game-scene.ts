import {
    AmbientLight,
    BoxGeometry,
    Clock,
    Color,
    Fog,
    MOUSE,
    Mesh,
    MeshLambertMaterial,
    PerspectiveCamera,
    Scene,
    WebGLRenderer,
    sRGBEncoding,
} from 'three';
import { AutoFollow } from './auto-follow';
import { Character } from './character';
import { OrbitControls } from '@three-ts/orbit-controls';
import { Sun } from './sun';
import { Updatable } from './updatable';
import { addCredit } from '../temp-util';
import { makeGround } from './three-util';

addCredit('<a target="_blank" href="https://threejs.org/">Three.js</a>');

export class GameScene {
    private readonly scene: THREE.Scene = new Scene();
    private readonly camera: THREE.PerspectiveCamera;
    private readonly renderer: THREE.WebGLRenderer = new WebGLRenderer({ antialias: true });

    private readonly toUpdate: Updatable[];

    private run = true;
    private requestedAnimationFrame?: number;

    private readonly clock = new Clock();

    constructor(wrapperElement: HTMLElement) {
        this.scene.background = new Color(0xcce0ff);
        this.scene.fog = new Fog(0xcce0ff, 500, 2000);

        this.camera = new PerspectiveCamera(100, window.innerWidth / window.innerHeight, 1, 10000);
        this.camera.position.set(-100, 50, -200);
        this.camera.position.multiplyScalar(2);

        const character = new Character();
        this.scene.add(character);

        this.scene.add(new AmbientLight(0x666666));
        this.scene.add(makeGround());

        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        wrapperElement.appendChild(this.renderer.domElement);
        this.renderer.outputEncoding = sRGBEncoding;
        this.renderer.shadowMap.enabled = true;

        const cameraControls = new OrbitControls(this.camera, this.renderer.domElement);
        cameraControls.maxPolarAngle = Math.PI * 0.94;
        cameraControls.minDistance = 50;
        cameraControls.maxDistance = 1000;
        cameraControls.enablePan = false;
        cameraControls.enableDamping = true;
        cameraControls.dampingFactor = 0.1;
        cameraControls.mouseButtons = {
            ZOOM: MOUSE.MIDDLE,
            ORBIT: MOUSE.RIGHT,
            PAN: MOUSE.LEFT,
        };
        cameraControls.target = character.position;

        const bigBox = new Mesh(new BoxGeometry(200, 1000, 200), new MeshLambertMaterial({ color: 0x666666 }));
        bigBox.name = 'ShadowDummyBox';
        bigBox.receiveShadow = true;
        bigBox.castShadow = true;
        bigBox.position.set(100, 300.0, 150);
        this.scene.add(bigBox);

        const sun = new Sun(50);
        sun.target = character;
        this.scene.add(sun);
        this.toUpdate = [character, cameraControls, new AutoFollow(sun, character)];

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
        const time = this.clock.getElapsedTime();
        for (const updatable of this.toUpdate) {
            updatable.update(time, delta);
        }
        this.render();
    }

    private render() {
        this.renderer.render(this.scene, this.camera);
    }
}
