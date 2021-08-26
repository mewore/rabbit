import * as THREE from './three/three.module.js';
import { OrbitControls } from './three/jsm/controls/OrbitControls.js';
import { GLTFLoader } from './three/jsm/loaders/GLTFLoader.js';

let container;
let camera, globalScene, renderer;
let globalControls;

let character = undefined;

addCredit(`<a target="_blank" href="https://threejs.org/">Three.js</a>`);

init();
animate(0);

function init() {
    container = document.createElement('div');
    document.body.appendChild(container);

    // scene

    const scene = new THREE.Scene();
    globalScene = scene;
    scene.background = new THREE.Color(0xcce0ff);
    scene.fog = new THREE.Fog(0xcce0ff, 500, 10000);

    // camera

    camera = new THREE.PerspectiveCamera(100, window.innerWidth / window.innerHeight, 1, 10000);
    camera.position.set(100, 50, 200);

    // lights

    scene.add(new THREE.AmbientLight(0x666666));

    const light = new THREE.DirectionalLight(0xdfebff, 1);
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

    const lowerLight = new THREE.PointLight(0xdfebff, 2);
    lowerLight.position.set(50, 10, 100);
    scene.add(lowerLight);

    // ground

    const groundTexture = new THREE.TextureLoader().load('./assets/ground.png');
    groundTexture.wrapS = groundTexture.wrapT = THREE.RepeatWrapping;
    groundTexture.repeat.set(50, 50);
    groundTexture.anisotropy = 16;
    groundTexture.encoding = THREE.sRGBEncoding;
    groundTexture.magFilter = THREE.NearestFilter;

    const groundMaterial = new THREE.MeshLambertMaterial({
        map: groundTexture,
    });

    let groundMesh = new THREE.Mesh(new THREE.PlaneGeometry(20000, 20000), groundMaterial);
    groundMesh.position.y = -50;
    groundMesh.rotation.x = -Math.PI / 2;
    groundMesh.receiveShadow = true;
    scene.add(groundMesh);

    const dummyBox = new THREE.Mesh(new THREE.BoxGeometry(100, 100, 100), groundMaterial);
    dummyBox.receiveShadow = true;
    dummyBox.castShadow = true;
    dummyBox.position.set(10, -50, 50);
    scene.add(dummyBox);
    character = dummyBox;

    // model

    if (isReisen()) {
        new GLTFLoader().setPath('./assets/reisen/').load('scene.gltf', function (gltf) {
            const reisen = gltf.scene;
            const reisenSize = 10;
            reisen.scale.set(reisenSize, reisenSize, reisenSize);
            reisen.position.set(dummyBox.position.x, dummyBox.position.y, dummyBox.position.z);
            makeAllCastShadow(reisen);
            scene.add(reisen);
            scene.remove(dummyBox);
            character = reisen;
        });
    } else {
        new GLTFLoader().setPath('./assets/carrot/').load('scene.gltf', function (gltf) {
            const carrotSize = 50;
            const carrot = gltf.scene;
            carrot.scale.set(carrotSize, carrotSize, carrotSize);

            makeAllCastShadow(carrot);
            scene.add(carrot);
            scene.remove(dummyBox);
            character = carrot;
            addCredit(
                `<a href="https://sketchfab.com/3d-models/low-poly-carrot-31df366e091a4e64b9b0cfc1afc0145d" target="_blank">Carrot model</a>` +
                    ` by <a href="https://sketchfab.com/thepianomonster" target="_blank">thepianomonster</a>`
            );
        });
    }

    // renderer

    renderer = new THREE.WebGLRenderer({
        antialias: true,
    });
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.setSize(window.innerWidth, window.innerHeight);

    container.appendChild(renderer.domElement);

    renderer.outputEncoding = THREE.sRGBEncoding;

    renderer.shadowMap.enabled = true;

    // controls
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.maxPolarAngle = Math.PI * 0.6;
    controls.minDistance = 50;
    controls.maxDistance = 1000;
    controls.enableDamping = true;
    controls.dampingFactor = 0.1;
    globalControls = controls;

    window.addEventListener('resize', onWindowResize);
}

function makeAllCastShadow(scene) {
    scene.traverse((node) => {
        if (node.isMesh && ['Outline', 'Iris', 'Gloss'].indexOf(node.material.name) === -1) {
            node.castShadow = true;
        }
    });
}

function onWindowResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();

    renderer.setSize(window.innerWidth, window.innerHeight);

    render();
}

function addCredit(html) {
    const newElement = document.createElement('div');
    newElement.innerHTML = html;
    document.getElementById('footer').appendChild(newElement);
}

function animate(now) {
    requestAnimationFrame(animate);
    simulate(now);
    render();
}

function simulate(now) {
    globalControls.update();
    if (character) {
        character.rotation.y = now * 0.001;
    }
}

function isReisen() {
    return document.title === 'Reisen';
}

function render() {
    renderer.render(globalScene, camera);
}
