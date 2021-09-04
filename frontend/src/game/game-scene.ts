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
import { AxisHelper, makeGround } from './three-util';
import { AutoFollow } from './auto-follow';
import { Character } from './character';
import { OrbitControls } from '@three-ts/orbit-controls';
import { Sun } from './sun';
import { Updatable } from './updatable';
import { addCredit } from '../temp-util';

addCredit('<a target="_blank" href="https://threejs.org/">Three.js</a>');

export class GameScene {
    private readonly MAX_DELTA = 0.5;

    private readonly scene: THREE.Scene = new Scene();
    private readonly camera: THREE.PerspectiveCamera;
    private readonly renderer: THREE.WebGLRenderer = new WebGLRenderer({ antialias: true });

    private readonly toUpdate: Updatable[];
    private readonly character = new Character();

    private run = false;
    private requestedAnimationFrame?: number;

    private readonly clock = new Clock();

    readonly inputs = {
        up: false,
        down: false,
        left: false,
        right: false,
    };

    constructor(wrapperElement: HTMLElement) {
        this.scene.background = new Color(0xcce0ff);
        this.scene.fog = new Fog(0xcce0ff, 500, 8000);

        this.camera = new PerspectiveCamera(80, window.innerWidth / window.innerHeight, 1, 10000);
        this.camera.position.set(-100, 50, -200);
        this.camera.position.multiplyScalar(2);

        this.scene.add(this.character);
        this.run = true;
        wrapperElement.tabIndex = 0;
        wrapperElement.focus();
        wrapperElement.appendChild(this.renderer.domElement);
        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.renderer.outputEncoding = sRGBEncoding;
        this.renderer.shadowMap.enabled = true;
        this.requestedAnimationFrame = requestAnimationFrame(this.animate.bind(this));

        this.scene.add(new AmbientLight(0x666666));
        this.scene.add(makeGround());

        const cameraControls = new OrbitControls(this.camera, this.renderer.domElement);
        cameraControls.minPolarAngle = Math.PI * 0.1;
        cameraControls.maxPolarAngle = Math.PI * 0.4;
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

        cameraControls.target = this.character.position;

        const shadowDummyBox = new Mesh(new BoxGeometry(200, 1000, 200), new MeshLambertMaterial({ color: 0x666666 }));
        shadowDummyBox.name = 'ShadowDummyBox';
        shadowDummyBox.receiveShadow = true;
        shadowDummyBox.castShadow = true;
        shadowDummyBox.position.set(300, 500.0, -100);
        this.scene.add(shadowDummyBox);

        const sun = new Sun(50);
        sun.target = this.character;
        this.scene.add(sun);
        this.scene.add(new AxisHelper());
        this.toUpdate = [this.character, cameraControls, new AutoFollow(sun, this.character)];

        window.addEventListener('resize', this.onWindowResize.bind(this));
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

        if (this.run) {
            this.render();
        }
    }

    private animate() {
        if (!this.run) {
            return;
        }
        this.requestMovement(
            (this.inputs.right ? 1 : 0) - (this.inputs.left ? 1 : 0),
            (this.inputs.down ? 1 : 0) - (this.inputs.up ? 1 : 0)
        );
        this.requestedAnimationFrame = requestAnimationFrame(this.animate.bind(this));
        const delta = Math.min(this.clock.getDelta(), this.MAX_DELTA);
        for (const updatable of this.toUpdate) {
            updatable.update(delta);
        }
        this.render();
    }

    private render() {
        this.renderer.render(this.scene, this.camera);
    }

    requestMovement(dx: number, dy: number): void {
        if (Math.abs(dx) > 0 || Math.abs(dy) > 0) {
            this.character.move(this.camera, dy, dx);
        } else {
            this.character.stopMoving();
        }
    }
}
