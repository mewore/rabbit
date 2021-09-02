import {
    AmbientLight,
    DirectionalLight,
    Light,
    Mesh,
    MeshLambertMaterial,
    NearestFilter,
    Object3D,
    PlaneGeometry,
    PointLight,
    RepeatWrapping,
    TextureLoader,
    sRGBEncoding,
} from 'three';

export function makeAllCastShadow(scene: THREE.Group): void {
    scene.traverse((node: THREE.Object3D) => {
        if (shouldNodeCastShadow(node)) {
            node.castShadow = true;
        }
    });
}

function shouldNodeCastShadow(node: THREE.Object3D): boolean {
    return isNodeMesh(node) && [node.material].flat().some((material) => shouldMaterialCastShadow(material));
}

function isNodeMesh(node: THREE.Object3D): node is THREE.Mesh {
    return (node as THREE.Mesh).isMesh;
}

function shouldMaterialCastShadow(material: THREE.Material): boolean {
    return ['Outline', 'Iris', 'Gloss'].indexOf(material.name) === -1;
}

export function makeDefaultLights(): Light[] {
    const sun = new DirectionalLight(0xdfebff, 1);
    sun.position.set(50, 200, 100);
    sun.position.multiplyScalar(1.3);

    sun.castShadow = true;

    sun.shadow.mapSize.width = 1024;
    sun.shadow.mapSize.height = 1024;

    const lightShadowPadding = 300;
    sun.shadow.camera.left = -lightShadowPadding;
    sun.shadow.camera.right = lightShadowPadding;
    sun.shadow.camera.top = lightShadowPadding;
    sun.shadow.camera.bottom = -lightShadowPadding;

    sun.shadow.camera.far = 1000;

    const lowerLight = new PointLight(0xdfebff, 2);
    lowerLight.position.set(50, 10, 100);

    return [sun, lowerLight, new AmbientLight(0x666666)];
}

export function makeGround(): Object3D {
    const groundMaterial = new MeshLambertMaterial();
    groundMaterial.name = 'GroundMaterial';

    new TextureLoader().loadAsync('./assets/ground.png').then((groundTexture) => {
        groundTexture.wrapS = groundTexture.wrapT = RepeatWrapping;
        groundTexture.repeat.set(500, 500);
        groundTexture.anisotropy = 16;
        groundTexture.encoding = sRGBEncoding;
        groundTexture.magFilter = NearestFilter;
        groundMaterial.map = groundTexture;
        groundMaterial.needsUpdate = true;
    });

    const groundMesh = new Mesh(new PlaneGeometry(200000, 200000), groundMaterial);
    groundMesh.name = 'Ground';
    groundMesh.rotation.x = -Math.PI / 2;
    groundMesh.receiveShadow = true;
    return groundMesh;
}
