import { Bone, Skeleton, SkinnedMesh } from 'three';
import { GLTF, GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader';

export type ClonedGltf = Pick<GLTF, 'animations' | 'scene'>;

const gltfRequestByUrl = new Map<string, Promise<GLTF>>();

/**
 * Load a GLTF scene. If it has already been loaded before, reuse the previously loaded one instead of sending any new
 * requests and re-parsing it. All GLTF data coming from here is cloned from the original.
 *
 * @param url The URL to fetch the GLTF scene from.
 * @returns The loaded scene.
 */
export async function loadGltfWithCaching(url: string): Promise<ClonedGltf> {
    let gltfRequest = gltfRequestByUrl.get(url);
    if (!gltfRequest) {
        gltfRequest = new GLTFLoader().loadAsync(url);
        gltfRequestByUrl.set(url, gltfRequest);
    }
    return cloneGltf(await gltfRequest);
}

/**
 * Clone a GLTF scene.
 *
 * Source: https://gist.github.com/cdata/f2d7a6ccdec071839bc1954c32595e87
 *
 * @param gltf The GLTF data to clone.
 * @returns The cloned GLTF scene, along with the original animations matched to the clone.
 */
function cloneGltf(gltf: GLTF): ClonedGltf {
    const clone = {
        animations: gltf.animations,
        scene: gltf.scene.clone(true),
    };

    const skinnedMeshes: { [name: string]: SkinnedMesh } = {};

    gltf.scene.traverse((node) => {
        if (node instanceof SkinnedMesh) {
            skinnedMeshes[node.name] = node;
        }
    });

    const cloneBones: { [name: string]: Bone } = {};
    const cloneSkinnedMeshes: { [name: string]: SkinnedMesh } = {};

    clone.scene.traverse((node) => {
        if (node instanceof Bone) {
            cloneBones[node.name] = node;
        }

        if (node instanceof SkinnedMesh) {
            cloneSkinnedMeshes[node.name] = node;
        }
    });

    for (const name in skinnedMeshes) {
        const skinnedMesh = skinnedMeshes[name];
        const skeleton = skinnedMesh.skeleton;
        const cloneSkinnedMesh = cloneSkinnedMeshes[name];

        const orderedCloneBones = [];

        for (let i = 0; i < skeleton.bones.length; ++i) {
            const cloneBone = cloneBones[skeleton.bones[i].name];
            orderedCloneBones.push(cloneBone);
        }

        cloneSkinnedMesh.bind(new Skeleton(orderedCloneBones, skeleton.boneInverses), cloneSkinnedMesh.matrixWorld);
    }

    return clone;
}
