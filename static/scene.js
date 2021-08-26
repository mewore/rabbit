import * as THREE from './three/three.module.js';
import { OrbitControls } from './three/jsm/controls/OrbitControls.js';
import { GLTFLoader } from './three/jsm/loaders/GLTFLoader.js';
import { EffectComposer } from './three/jsm/postprocessing/EffectComposer.js';
import { RenderPass } from './three/jsm/postprocessing/RenderPass.js';
import { ShaderPass } from './three/jsm/postprocessing/ShaderPass.js';
import { GUI } from './three/jsm/libs/dat.gui.module.js';

let container;
let camera, globalScene, renderer, globalComposer;
let globalControls;

let character = undefined;

const params = {
    fisheye: false,
};

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
    groundTexture.repeat.set(500, 500);
    groundTexture.anisotropy = 16;
    groundTexture.encoding = THREE.sRGBEncoding;
    groundTexture.magFilter = THREE.NearestFilter;

    const groundMaterial = new THREE.MeshLambertMaterial({
        map: groundTexture,
    });

    let groundMesh = new THREE.Mesh(new THREE.PlaneGeometry(200000, 200000), groundMaterial);
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

    // Create effect composer
    const composer = new EffectComposer(renderer);
    composer.addPass(new RenderPass(scene, camera));

    // Add distortion effect to effect composer
    const effect = new ShaderPass(getFisheyeShaderDefinition());
    composer.addPass(effect);
    effect.renderToScreen = true;
    globalComposer = composer;

    // Setup distortion effect
    const horizontalFOV = 150;
    const fisheyeStrength = 1.0;
    const cylindricalRatio = 0.3;
    const fisheyeHeight = Math.tan(THREE.Math.degToRad(horizontalFOV) / 2) / camera.aspect;

    camera.fov = (Math.atan(fisheyeHeight) * 2 * 180) / 3.1415926535;
    camera.updateProjectionMatrix();

    effect.uniforms['strength'].value = fisheyeStrength;
    effect.uniforms['height'].value = fisheyeHeight;
    effect.uniforms['aspectRatio'].value = camera.aspect;
    effect.uniforms['cylindricalRatio'].value = cylindricalRatio;

    // controls
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.maxPolarAngle = Math.PI * 0.6;
    controls.minDistance = 50;
    controls.maxDistance = 1000;
    controls.enableDamping = true;
    controls.dampingFactor = 0.1;
    globalControls = controls;

    window.addEventListener('resize', onWindowResize);

    const gui = new GUI();
    gui.add(params, 'fisheye').name('Enable fisheye effect');
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
    if (params.fisheye) {
        globalComposer.render();
    }
}

// Made by Giliam de Carpentier: https://www.decarpentier.nl/lens-distortion
// Actually useful for much more than just funny fumo faces
function getFisheyeShaderDefinition() {
    return {
        uniforms: {
            tDiffuse: { type: 't', value: null },
            strength: { type: 'f', value: 0 },
            height: { type: 'f', value: 1 },
            aspectRatio: { type: 'f', value: 1 },
            cylindricalRatio: { type: 'f', value: 1 },
        },

        vertexShader: [
            'uniform float strength;', // s: 0 = perspective, 1 = stereographic
            'uniform float height;', // h: tan(verticalFOVInRadians / 2)
            'uniform float aspectRatio;', // a: screenWidth / screenHeight
            'uniform float cylindricalRatio;', // c: cylindrical distortion ratio. 1 = spherical

            'varying vec3 vUV;', // output to interpolate over screen
            'varying vec2 vUVDot;', // output to interpolate over screen

            'void main() {',
            'gl_Position = projectionMatrix * (modelViewMatrix * vec4(position, 1.0));',

            'float scaledHeight = strength * height;',
            'float cylAspectRatio = aspectRatio * cylindricalRatio;',
            'float aspectDiagSq = aspectRatio * aspectRatio + 1.0;',
            'float diagSq = scaledHeight * scaledHeight * aspectDiagSq;',
            'vec2 signedUV = (2.0 * uv + vec2(-1.0, -1.0));',

            'float z = 0.5 * sqrt(diagSq + 1.0) + 0.5;',
            'float ny = (z - 1.0) / (cylAspectRatio * cylAspectRatio + 1.0);',

            'vUVDot = sqrt(ny) * vec2(cylAspectRatio, 1.0) * signedUV;',
            'vUV = vec3(0.5, 0.5, 1.0) * z + vec3(-0.5, -0.5, 0.0);',
            'vUV.xy += uv;',
            '}',
        ].join('\n'),

        fragmentShader: [
            'uniform sampler2D tDiffuse;', // sampler of rendered scene?s render target
            'varying vec3 vUV;', // interpolated vertex output data
            'varying vec2 vUVDot;', // interpolated vertex output data

            'void main() {',
            'vec3 uv = dot(vUVDot, vUVDot) * vec3(-0.5, -0.5, -1.0) + vUV;',
            'gl_FragColor = texture2DProj(tDiffuse, uv);',
            '}',
        ].join('\n'),
    };
}
