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
    Object3D,
    OrthographicCamera,
    PerspectiveCamera,
    PlaneBufferGeometry,
    RepeatWrapping,
    TextureLoader,
    Vector2,
    Vector3,
    sRGBEncoding,
} from 'three';

const GROUND_TEXTURE_SCALE = 1 / 16;

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

export function makeGround(worldWidth: number, worldDepth: number): Object3D {
    const groundMaterial = new MeshStandardMaterial();
    groundMaterial.name = 'GroundMaterial';
    groundMaterial.side = FrontSide;

    const meshSize = new Vector2(worldWidth * 3, worldDepth * 3);
    const groundMesh = new Mesh(
        new PlaneBufferGeometry(meshSize.x, meshSize.y, Math.ceil(worldWidth / 1000), Math.ceil(worldDepth / 1000)),
        groundMaterial
    );

    const textureLoader = new TextureLoader();
    textureLoader.loadAsync('./assets/dirt.jpg').then((groundTexture) => {
        const textureSize = new Vector2(groundTexture.image.width, groundTexture.image.height);
        const scaledTextureSize = new Vector2().copy(textureSize).multiplyScalar(GROUND_TEXTURE_SCALE);

        groundTexture.wrapS = groundTexture.wrapT = RepeatWrapping;
        groundTexture.repeat.copy(meshSize).divide(scaledTextureSize).divideScalar(3).round().multiplyScalar(3);
        groundTexture.anisotropy = 8;
        groundTexture.encoding = sRGBEncoding;
        groundTexture.minFilter = LinearMipMapLinearFilter;
        groundMaterial.map = groundTexture;
        groundMaterial.needsUpdate = true;

        const actualTextureSize = new Vector2().copy(meshSize).divide(groundTexture.repeat);
        if (!actualTextureSize.equals(scaledTextureSize)) {
            const actualTextureSizePercentage = new Vector2()
                .copy(actualTextureSize)
                .divide(scaledTextureSize)
                .multiplyScalar(10000)
                .round()
                .divideScalar(100);
            actualTextureSize.multiplyScalar(100).round().divideScalar(100);
            const suggestedWorldSize = new Vector2()
                .copy(groundTexture.repeat)
                .multiply(scaledTextureSize)
                .divideScalar(3);
            window.console.warn(
                `The ground texture is scaled from the intended ${scaledTextureSize.x} x ${scaledTextureSize.y} px ` +
                    `to ${actualTextureSize.x} x ${actualTextureSize.y} px ` +
                    `(${actualTextureSizePercentage.x}% x ${actualTextureSizePercentage.y}%). ` +
                    'The following world size would eliminate the stretching: ' +
                    `(${suggestedWorldSize.x}, ${suggestedWorldSize.y})`
            );
        }
    });

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
