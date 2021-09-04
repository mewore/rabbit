import {
    BackSide,
    BoxGeometry,
    LinearMipMapLinearFilter,
    Material,
    Mesh,
    MeshLambertMaterial,
    MeshStandardMaterial,
    MirroredRepeatWrapping,
    Object3D,
    PlaneGeometry,
    TextureLoader,
    Vector2,
    Vector3,
    sRGBEncoding,
} from 'three';

const TAU = Math.PI * 2;
const tmpVector3 = new Vector3();
const otherTmpVector3 = new Vector3();

export function makeAllCastAndReceiveShadow(scene: THREE.Group): void {
    scene.traverse((node: THREE.Object3D) => {
        if (shouldNodeCastShadow(node)) {
            node.castShadow = true;
            if (node instanceof Mesh && node.material instanceof Material) {
                node.material.shadowSide = BackSide;
            }
        }
        node.receiveShadow = true;
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
    const groundMaterial = new MeshStandardMaterial();
    groundMaterial.name = 'GroundMaterial';

    const targetMeshSize = new Vector2(200000, 200000);
    const groundMesh = new Mesh(new PlaneGeometry(targetMeshSize.x, targetMeshSize.y), groundMaterial);

    (async (): Promise<void> => {
        const textureLoader = new TextureLoader();
        const [groundTexture, groundRoughnessTexture, groundBumpMap] = await Promise.all([
            textureLoader.loadAsync('./assets/ground.png'),
            textureLoader.loadAsync('./assets/ground-roughness.png'),
            textureLoader.loadAsync('./assets/ground-bumpmap.png'),
        ]);
        const textureScale = 0.125;
        const textureSize = new Vector2(groundTexture.image.width, groundTexture.image.height);
        groundTexture.wrapS = groundTexture.wrapT = MirroredRepeatWrapping;
        groundTexture.repeat.copy(targetMeshSize).divide(textureSize).divideScalar(textureScale).floor();
        groundTexture.anisotropy = 16;
        groundTexture.encoding = sRGBEncoding;
        groundTexture.minFilter = LinearMipMapLinearFilter;

        for (const otherTexture of [groundRoughnessTexture, groundBumpMap]) {
            otherTexture.wrapS = otherTexture.wrapT = MirroredRepeatWrapping;
            otherTexture.repeat.copy(groundTexture.repeat);
            otherTexture.anisotropy = groundTexture.anisotropy = 16;
            otherTexture.encoding = groundTexture.encoding = sRGBEncoding;
            otherTexture.minFilter = groundTexture.minFilter;
        }

        groundMaterial.map = groundTexture;
        groundMaterial.needsUpdate = true;

        groundMaterial.metalness = 0.2;
        groundMaterial.roughnessMap = groundRoughnessTexture;

        groundMaterial.bumpMap = groundBumpMap;
        groundMaterial.bumpScale = 1 / textureScale;

        groundMesh.geometry = new PlaneGeometry(
            groundTexture.repeat.x * textureSize.x * textureScale,
            groundTexture.repeat.y * textureSize.y * textureScale
        );
    })();

    groundMesh.name = 'Ground';
    groundMesh.rotation.x = -Math.PI / 2;
    groundMesh.receiveShadow = true;
    return groundMesh;
}

// TODO: Optimize
export function wrapAngle(angle: number): number {
    while (angle > TAU) {
        angle -= TAU;
    }
    while (angle < 0) {
        angle += TAU;
    }
    return angle;
}

export class AxisHelper extends Object3D {
    constructor() {
        super();
        this.name = 'AxisHelper';

        const xBox = new Mesh(new BoxGeometry(50, 10, 20), new MeshLambertMaterial({ color: 'red' }));
        xBox.name = 'X-Box';
        xBox.receiveShadow = true;
        xBox.castShadow = true;
        xBox.position.setX(50);
        xBox.position.setY(5);
        this.attach(xBox);

        const zBox = new Mesh(new BoxGeometry(20, 10, 50), new MeshLambertMaterial({ color: 'blue' }));
        zBox.name = 'Z-Box';
        zBox.receiveShadow = true;
        zBox.castShadow = true;
        zBox.position.setZ(50);
        zBox.position.setY(5);
        this.attach(zBox);
    }
}

export function globalVector(from: Object3D, to: Object3D): Vector3 {
    return to.localToWorld(tmpVector3.set(0, 0, 0)).sub(from.localToWorld(otherTmpVector3.set(0, 0, 0)));
}
