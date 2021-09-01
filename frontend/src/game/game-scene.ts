import {
    AmbientLight,
    BoxGeometry,
    Color,
    DirectionalLight,
    Fog,
    Mesh,
    MeshLambertMaterial,
    NearestFilter,
    PerspectiveCamera,
    PlaneGeometry,
    PointLight,
    RepeatWrapping,
    Scene,
    TextureLoader,
    WebGLRenderer,
    sRGBEncoding,
} from 'three';
import { GameRuntime } from './game-runtime';
import { OrbitControls } from '@three-ts/orbit-controls';
import { addCredit } from '../temp-util';

addCredit('<a target="_blank" href="https://threejs.org/">Three.js</a>');

export class GameScene {
    readonly runtime: GameRuntime;

    constructor(wrapperElement: HTMLElement) {
        const scene = new Scene();
        scene.background = new Color(0xcce0ff);
        scene.fog = new Fog(0xcce0ff, 500, 10000);

        const camera = new PerspectiveCamera(100, window.innerWidth / window.innerHeight, 1, 10000);
        camera.position.set(100, 50, 200);

        scene.add(new AmbientLight(0x666666));

        const light = new DirectionalLight(0xdfebff, 1);
        light.position.set(50, 200, 100);
        light.position.multiplyScalar(1.3);

        light.castShadow = true;

        light.shadow.mapSize.width = 1024;
        light.shadow.mapSize.height = 1024;

        const lightShadowPadding = 300;
        light.shadow.camera.left = -lightShadowPadding;
        light.shadow.camera.right = lightShadowPadding;
        light.shadow.camera.top = lightShadowPadding;
        light.shadow.camera.bottom = -lightShadowPadding;

        light.shadow.camera.far = 1000;

        scene.add(light);

        const lowerLight = new PointLight(0xdfebff, 2);
        lowerLight.position.set(50, 10, 100);
        scene.add(lowerLight);

        const groundTexture = new TextureLoader().load('./assets/ground.png');
        groundTexture.wrapS = groundTexture.wrapT = RepeatWrapping;
        groundTexture.repeat.set(500, 500);
        groundTexture.anisotropy = 16;
        groundTexture.encoding = sRGBEncoding;
        groundTexture.magFilter = NearestFilter;

        const groundMaterial = new MeshLambertMaterial({
            map: groundTexture,
        });

        const groundMesh = new Mesh(new PlaneGeometry(200000, 200000), groundMaterial);
        groundMesh.position.y = -50;
        groundMesh.rotation.x = -Math.PI / 2;
        groundMesh.receiveShadow = true;
        scene.add(groundMesh);

        const dummyBox = new Mesh(new BoxGeometry(100, 100, 100), groundMaterial);
        dummyBox.receiveShadow = true;
        dummyBox.castShadow = true;
        dummyBox.position.set(10, -50, 50);
        scene.add(dummyBox);

        const renderer = new WebGLRenderer({
            antialias: true,
        });
        renderer.setPixelRatio(window.devicePixelRatio);
        renderer.setSize(window.innerWidth, window.innerHeight);
        wrapperElement.appendChild(renderer.domElement);
        renderer.outputEncoding = sRGBEncoding;
        renderer.shadowMap.enabled = true;

        const controls = new OrbitControls(camera, renderer.domElement);
        controls.maxPolarAngle = Math.PI * 0.4;
        controls.minDistance = 50;
        controls.maxDistance = 1000;
        controls.enablePan = false;
        controls.enableDamping = true;
        controls.dampingFactor = 0.1;

        this.runtime = new GameRuntime(camera, scene, renderer, controls, dummyBox);
    }
}
