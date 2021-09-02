import {
    BackSide,
    Material,
    Mesh,
    MeshLambertMaterial,
    NearestFilter,
    Object3D,
    PlaneGeometry,
    RepeatWrapping,
    TextureLoader,
    sRGBEncoding,
} from 'three';

export function makeAllCastAndReceiveShadow(scene: THREE.Group): void {
    window.console.log(scene);
    scene.traverse((node: THREE.Object3D) => {
        if (shouldNodeCastShadow(node)) {
            node.castShadow = true;
        }
        node.receiveShadow = true;
        if (node instanceof Mesh && node.material instanceof Material) {
            window.console.log(node);
            node.material.shadowSide = BackSide;
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
