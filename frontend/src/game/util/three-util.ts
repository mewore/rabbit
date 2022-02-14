import {
    BackSide,
    BoxBufferGeometry,
    Camera,
    CubeTexture,
    CubeTextureLoader,
    FrontSide,
    LinearMipMapLinearFilter,
    Material,
    Mesh,
    MeshLambertMaterial,
    MeshStandardMaterial,
    MirroredRepeatWrapping,
    Object3D,
    OrthographicCamera,
    PerspectiveCamera,
    PlaneBufferGeometry,
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
    groundMaterial.side = FrontSide;

    const targetMeshSize = new Vector2(20000, 20000);
    const groundMesh = new Mesh(new PlaneGeometry(targetMeshSize.x, targetMeshSize.y), groundMaterial);

    (async (): Promise<void> => {
        const textureLoader = new TextureLoader();
        const [groundTexture, groundRoughnessTexture, groundBumpMap] = await Promise.all([
            textureLoader.loadAsync('./assets/ground.png'),
            textureLoader.loadAsync('./assets/ground-roughness.png'),
            textureLoader.loadAsync('./assets/ground-bumpmap.png'),
        ]);
        const textureScale = 1 / 64;
        const textureSize = new Vector2(groundTexture.image.width, groundTexture.image.height);
        groundTexture.wrapS = groundTexture.wrapT = MirroredRepeatWrapping;
        groundTexture.repeat.copy(targetMeshSize).divide(textureSize).divideScalar(textureScale).floor();
        groundTexture.anisotropy = 8;
        groundTexture.encoding = sRGBEncoding;
        groundTexture.minFilter = LinearMipMapLinearFilter;

        for (const otherTexture of [groundRoughnessTexture, groundBumpMap]) {
            otherTexture.wrapS = otherTexture.wrapT = MirroredRepeatWrapping;
            otherTexture.repeat.copy(groundTexture.repeat);
            otherTexture.anisotropy = groundTexture.anisotropy = 8;
            otherTexture.encoding = groundTexture.encoding = sRGBEncoding;
            otherTexture.minFilter = groundTexture.minFilter;
        }

        groundMaterial.map = groundTexture;
        groundMaterial.needsUpdate = true;

        groundMaterial.metalness = 0.2;
        groundMaterial.roughnessMap = groundRoughnessTexture;

        groundMaterial.bumpMap = groundBumpMap;

        groundMesh.geometry.dispose();
        groundMesh.geometry = new PlaneBufferGeometry(
            groundTexture.repeat.x * textureSize.x * textureScale,
            groundTexture.repeat.y * textureSize.y * textureScale
        );
    })();

    groundMesh.name = 'Ground';
    groundMesh.rotateX(-Math.PI / 2);
    groundMesh.updateMatrix();
    groundMesh.updateMatrixWorld();
    groundMesh.receiveShadow = true;
    return groundMesh;
}

export async function makeSkybox(): Promise<CubeTexture> {
    const imgArray = ['left', 'right', 'top', 'bottom', 'front', 'back'];

    const urls = imgArray.map((side) => '/assets/sky/skybox/skybox_' + side + '.PNG');
    return new Promise((resolve) => {
        new CubeTextureLoader().load(urls, (texture) => {
            resolve(texture);
        });
    });
}

export function wrap(value: number, min: number, max: number): number {
    let normalized = ((value - min) / (max - min)) % 1;
    if (normalized < 0) {
        normalized++;
    }
    return min + (max - min) * normalized;
}

export function wrapAngle(angle: number): number {
    if (angle >= TAU) {
        return angle - Math.floor(angle / TAU) * TAU;
    } else if (angle < 0) {
        return angle + Math.floor(-angle / TAU) * TAU;
    }
    return angle;
}

export function getAngleDifference(from: number, to: number): number {
    return from > to ? Math.min(from - to, to + TAU - from) : Math.min(to - from, from + TAU - to);
}

export function moveAngle(from: number, to: number, maxMovement: number): number {
    from = wrapAngle(from);
    to = wrapAngle(to);

    const positiveDifference = wrapAngle(to > from ? to - from : to - from + TAU);
    const negativeDifference = -wrapAngle(from > to ? from - to : from - to + TAU);

    const bestDifference = positiveDifference < -negativeDifference ? positiveDifference : -negativeDifference;
    const sign = positiveDifference < -negativeDifference ? 1 : -1;
    return wrapAngle(from + Math.min(bestDifference, maxMovement) * sign);
}

export class AxisHelper extends Object3D {
    constructor() {
        super();
        this.name = 'AxisHelper';

        const xBox = new Mesh(new BoxBufferGeometry(5, 1, 2), new MeshLambertMaterial({ color: 'red' }));
        xBox.name = 'X-Box';
        xBox.receiveShadow = true;
        xBox.castShadow = true;
        xBox.position.setX(5);
        xBox.position.setY(0.5);
        this.attach(xBox);

        const zBox = new Mesh(new BoxBufferGeometry(2, 1, 5), new MeshLambertMaterial({ color: 'blue' }));
        zBox.name = 'Z-Box';
        zBox.receiveShadow = true;
        zBox.castShadow = true;
        zBox.position.setZ(5);
        zBox.position.setY(0.5);
        this.attach(zBox);
    }
}

export function globalVector(from: Object3D, to: Object3D): Vector3 {
    return to.localToWorld(tmpVector3.set(0, 0, 0)).sub(from.localToWorld(otherTmpVector3.set(0, 0, 0)));
}

export function projectOntoCamera(worldPosition: Vector3, camera: Camera): Vector3 | undefined {
    const distance =
        camera instanceof PerspectiveCamera || camera instanceof OrthographicCamera
            ? camera.worldToLocal(tmpVector3.copy(worldPosition)).length() / camera.far
            : 0;
    const vector = tmpVector3.copy(worldPosition).project(camera);
    if (vector.z > 1.0) {
        // The point is behind the camera
        return undefined;
    }
    return vector.set((vector.x + 1) / 2, (-vector.y + 1) / 2, distance);
}
