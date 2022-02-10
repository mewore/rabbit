import {
    AmbientLight,
    Clock,
    Color,
    Fog,
    HemisphereLight,
    Mesh,
    MeshBasicMaterial,
    Object3D,
    PerspectiveCamera,
    PointLight,
    Scene,
    SphereBufferGeometry,
    Vector3,
    WebGLRenderer,
} from 'three';
import { AxisHelper, makeGround, makeSkybox, projectOntoCamera, wrap } from './util/three-util';
import { Body, Material, Plane, Quaternion, Vec3, World } from 'cannon-es';
import { addCredit, isReisen } from '../temp-util';
import { AutoFollow } from './util/auto-follow';
import { Character } from './character';
import { FixedDistanceOrbitControls } from './util/fixed-distance-orbit-controls';
import { ForestDataMessage } from './entities/messages/forest-data-message';
import { ForestObject } from './forest/forest-object';
import { GroundBox } from './util/ground-box';
import { Input } from './util/input';
import { Moon } from './moon';
import { PlayerDisconnectMessage } from './entities/messages/player-disconnect-message';
import { PlayerJoinMessage } from './entities/messages/player-join-message';
import { PlayerJoinMutation } from './entities/mutations/player-join-mutation';
import { PlayerUpdateMessage } from './entities/messages/player-update-message';
import { PlayerUpdateMutation } from './entities/mutations/player-update-mutation';
import { SignedBinaryReader } from './entities/data/signed-binary-reader';
import { Updatable } from './util/updatable';
import { Wrappable } from './util/wrappable';
import { degToRad } from 'three/src/math/MathUtils';

enum MessageType {
    JOIN,
    FOREST_DATA,
    UPDATE,
    DISCONNECT,
}

addCredit({
    thing: { text: 'Three.js', url: 'https://threejs.org/' },
    author: { text: 'Mr.doob', url: 'https://github.com/mrdoob' },
});

addCredit({
    thing: { text: 'cannon-es', url: 'https://github.com/pmndrs/cannon-es' },
    author: { text: 'Stefan Hedman (and Poimandres)', url: 'https://github.com/schteppe' },
});

const GROUND_TEXTURE_SCALE = 1 / 64;
const ASSUMED_GROUND_TEXTURE_WIDTH = 512 * 2 * GROUND_TEXTURE_SCALE;
const ASSUMED_GROUND_TEXTURE_HEIGHT = 880 * 2 * GROUND_TEXTURE_SCALE;
const DESIRED_WORLD_SIZE = 1000;
const WORLD_WIDTH = Math.round(DESIRED_WORLD_SIZE / ASSUMED_GROUND_TEXTURE_WIDTH) * ASSUMED_GROUND_TEXTURE_WIDTH;
const WORLD_DEPTH = Math.round(DESIRED_WORLD_SIZE / ASSUMED_GROUND_TEXTURE_HEIGHT) * ASSUMED_GROUND_TEXTURE_HEIGHT;

const MIN_X = -WORLD_WIDTH / 2;
const MAX_X = WORLD_WIDTH / 2;
const MIN_Z = -WORLD_DEPTH / 2;
const MAX_Z = WORLD_DEPTH / 2;

const WRAP_X_OFFSETS = [-WORLD_WIDTH, 0, WORLD_WIDTH];
const WRAP_Z_OFFSETS = [-WORLD_DEPTH, 0, WORLD_DEPTH];

const WRAP_OFFSETS: Vector3[] = WRAP_X_OFFSETS.flatMap((xOffset) =>
    WRAP_Z_OFFSETS.map((zOffset) => new Vector3(xOffset, 0, zOffset))
);
const NONZERO_WRAP_OFFSETS: Vector3[] = WRAP_OFFSETS.filter((offset) => offset.lengthSq() > 0);

const PHYSICS_TIME_STEP_SIZE = 1 / 60;

const FOV = 60;
// The fog distance has to be adjusted based on the FOV because the fog opacity depends on the distance of the plane
//  perpendicular to the camera intersecting the object insead of on the distance between the camera and the object.
//  Fog is implemented like this in every 3D engine and it's so unrealistic, but I guess it's easier to calculate.
const FOG_END = Math.cos(degToRad(FOV / 2)) * 300;
const FOG_START = FOG_END * 0;

export class GameScene {
    private readonly MAX_DELTA = 0.5;

    private readonly scene: THREE.Scene = new Scene();
    private readonly camera = new PerspectiveCamera(FOV, 1, 1, 1000);
    private currentRenderer: WebGLRenderer = new WebGLRenderer({ antialias: true });
    get renderer(): WebGLRenderer {
        return this.currentRenderer;
    }

    private readonly updatableById = new Map<number | string, Updatable>();
    private readonly character = new Character('', isReisen());

    private readonly clock = new Clock();
    private readonly elapsedTimeClock = new Clock();

    private readonly charactersById = new Map<number, Character[]>();

    readonly input = new Input();

    private readonly cameraControls: FixedDistanceOrbitControls;

    private readonly physicsWorld = new World({ gravity: new Vec3(0, -250, 0), allowSleep: false });

    readonly forest = new ForestObject(WORLD_WIDTH, WORLD_DEPTH, WRAP_OFFSETS, this.input);

    private width = 0;
    private height = 0;

    constructor(private readonly wrapperElement: HTMLElement, private readonly webSocket: WebSocket) {
        this.scene.background = new Color(0x0b051b);
        this.scene.fog = new Fog(this.scene.background, FOG_START, FOG_END);
        this.camera.position.set(-30, 10, -50);
        this.camera.far = FOG_END * 2;
        this.cameraControls = new FixedDistanceOrbitControls(this.input, this.camera, this.character);
        this.camera.rotation.reorder('YXZ');
        this.forest.camera = this.camera;
        this.cameraControls.offset = new Vector3(0, 20, 0);

        makeSkybox().then((skybox) => (this.scene.background = skybox));

        this.add(this.character);
        this.wrapperElement.appendChild(this.currentRenderer.domElement);
        this.currentRenderer.setPixelRatio(window.devicePixelRatio * 0.5);
        this.currentRenderer.shadowMap.enabled = true;

        const centralLight = new PointLight(0xffdd44, 1, WORLD_WIDTH / 4, 0.9);
        centralLight.position.set(0, 10, 0);
        centralLight.castShadow = true;
        const lightSphere = new Mesh(new SphereBufferGeometry(1, 16, 16), new MeshBasicMaterial({ color: 0xffdd44 }));
        lightSphere.position.copy(centralLight.position);
        lightSphere.material.fog = false;

        this.add(centralLight);
        this.add(...this.cloneWithOffset(lightSphere));

        this.add(new AmbientLight(this.scene.background, 3));
        this.add(new AmbientLight(0x112255, 1));
        this.add(new HemisphereLight(this.scene.background, 0x154f30, 0.5));
        const ground = makeGround();
        this.add(ground);
        this.physicsWorld.addBody(
            new Body({
                shape: new Plane(),
                quaternion: new Quaternion().setFromEuler(-Math.PI / 2, 0, 0),
                material: new Material({ friction: 0, restitution: 0 }),
            })
        );

        // this.add(this.forest);

        const shadowDummyBox = new GroundBox(20, 100, 30, -10, Math.PI * 0.2);
        shadowDummyBox.name = 'ShadowDummyBox';
        this.add(...this.cloneWithOffset(shadowDummyBox));

        const cameraIntersectionObjects = [ground, shadowDummyBox];

        for (let i = 0; i < 10; i++) {
            const physicsDummyBox = new GroundBox(20, 10 + i * 10, -30 - i * 20 - i * i, -30);
            physicsDummyBox.name = 'PhysicsDummyBox' + i;
            this.add(...this.cloneWithOffset(physicsDummyBox));
            cameraIntersectionObjects.push(physicsDummyBox);
        }

        this.cameraControls.intersectionObjects = cameraIntersectionObjects;

        this.add(...this.cloneWithOffset(new AxisHelper()));

        const moon = new Moon(100);
        moon.target = this.character;
        this.add(moon);

        this.add(new AutoFollow(moon, this.character));
        this.add(this.cameraControls);

        this.webSocket.onmessage = (message: MessageEvent<ArrayBuffer>) => {
            const reader = new SignedBinaryReader(message.data);
            const messageType = reader.readByte();
            switch (messageType) {
                case MessageType.JOIN:
                    return this.onPlayerJoined(reader);
                case MessageType.UPDATE:
                    return this.onPlayerUpdate(reader);
                case MessageType.DISCONNECT:
                    return this.onPlayerDisconnected(reader);
                case MessageType.FOREST_DATA:
                    return this.onForestData(reader);
                default:
                    throw new Error('Unrecognized message type: ' + messageType);
            }
        };

        this.refreshSize();
        this.character.visible = false;
    }

    recreateRenderer(quality: number, shadows: boolean): void {
        if (shadows !== this.currentRenderer.shadowMap.enabled) {
            this.wrapperElement.removeChild(this.currentRenderer.domElement);
            this.currentRenderer.forceContextLoss();
            this.currentRenderer.dispose();

            this.currentRenderer = new WebGLRenderer({ antialias: true });
            this.wrapperElement.appendChild(this.currentRenderer.domElement);
            this.currentRenderer.shadowMap.enabled = shadows;
        }
        this.currentRenderer.setPixelRatio(window.devicePixelRatio * quality);
        this.refreshSize();
    }

    cloneWithOffset<T extends Object3D>(object: T): T[] {
        const result: T[] = [object];
        for (const offset of NONZERO_WRAP_OFFSETS) {
            const cloned = object.clone(false);
            cloned.position.add(offset);
            result.push(cloned);
        }
        return result;
    }

    get time(): number {
        return this.elapsedTimeClock.getElapsedTime();
    }

    start(): void {
        if (this.character.visible) {
            return;
        }
        this.character.visible = true;
        this.character.position.y = 300;
        this.updatableById.set(this.character.id, this.character);
        if (this.webSocket.readyState === WebSocket.OPEN) {
            this.sendInitialPlayerInfo();
        } else {
            this.webSocket.onopen = () => this.sendInitialPlayerInfo();
        }
    }

    private sendInitialPlayerInfo(): void {
        this.webSocket.send(new PlayerJoinMutation(isReisen()).encodeToBinary());
        this.webSocket.send(new PlayerUpdateMutation(this.character.getState()).encodeToBinary());
    }

    private onPlayerJoined(reader: SignedBinaryReader): void {
        const message = PlayerJoinMessage.decodeFromBinary(reader);
        const characters = this.getOrCreatePlayerCharacters(
            message.player.id,
            message.player.username,
            message.player.isReisen
        );
        for (const character of characters) {
            character.setState(message.player.state);
        }
    }

    private onPlayerUpdate(reader: SignedBinaryReader): void {
        const message = PlayerUpdateMessage.decodeFromBinary(reader);
        const characters = this.getOrCreatePlayerCharacters(message.playerId, 'Unknown', undefined);
        for (const character of characters) {
            character.setState(message.newState);
        }
    }

    private onPlayerDisconnected(reader: SignedBinaryReader): void {
        const message = PlayerDisconnectMessage.decodeFromBinary(reader);
        const characters = this.charactersById.get(message.playerId);
        if (!characters) {
            return;
        }
        this.remove(...characters);
        this.charactersById.delete(message.playerId);
    }

    private onForestData(reader: SignedBinaryReader): void {
        const message = ForestDataMessage.decodeFromBinary(reader);
        this.forest.setForestData(message.forest);
    }

    private getOrCreatePlayerCharacters(
        playerId: number,
        username: string,
        isReisen: boolean | undefined
    ): Character[] {
        const existingCharacters = this.charactersById.get(playerId);
        if (existingCharacters) {
            return existingCharacters;
        }
        const characters: Character[] = [];
        for (const offset of WRAP_OFFSETS) {
            const newCharacter = new Character(username, isReisen, offset);
            this.add(newCharacter);
            characters.push(newCharacter);
        }
        this.charactersById.set(playerId, characters);
        return characters;
    }

    animate(): void {
        this.requestMovement(this.input.movementRight, this.input.movementForwards);
        const now = this.time;
        const delta = Math.min(this.clock.getDelta(), this.MAX_DELTA);
        if (this.character.visible && this.input.wantsToJump) {
            this.character.requestJump(now);
        }

        if (delta > 0) {
            for (const updatable of this.updatableById.values()) {
                updatable.beforePhysics(delta, now);
            }

            this.physicsWorld.step(PHYSICS_TIME_STEP_SIZE, delta);
            for (const updatable of this.updatableById.values()) {
                updatable.afterPhysics(delta, now);
                if (this.isWrappable(updatable)) {
                    const position = updatable.position;
                    const offset = updatable.offset;
                    if (position.x < MIN_X + offset.x || position.x > MAX_X + offset.x) {
                        position.setX(wrap(position.x, MIN_X + offset.x, MAX_X + offset.x));
                    }
                    if (position.z < MIN_Z + offset.z || position.z > MAX_Z + offset.z) {
                        position.setZ(wrap(position.z, MIN_Z + offset.z, MAX_Z + offset.z));
                    }
                }
            }
        }

        for (const updatable of this.updatableById.values()) {
            updatable.update(delta, now);
        }

        for (const updatable of this.updatableById.values()) {
            updatable.beforeRender(delta, now);
        }
        this.render();

        this.input.clearMouseDelta();
        if (this.character.visible) {
            if (this.webSocket.readyState === WebSocket.OPEN && this.character.hasChanged()) {
                this.webSocket.send(new PlayerUpdateMutation(this.character.getState()).encodeToBinary());
            }
        }
    }

    private isWrappable(object: unknown): object is Wrappable {
        return !!(object as Wrappable).isWrappable;
    }

    private render() {
        this.currentRenderer.render(this.scene, this.camera);
    }

    private requestMovement(side: number, forwards: number): void {
        if (!this.character.visible) {
            return;
        }
        if (Math.abs(side) > 0 || Math.abs(forwards) > 0) {
            this.character.requestMovement(this.camera, forwards, side);
        } else {
            this.character.stopMoving();
        }
    }

    forEveryPlayerLabel(callback: (position: Vector3, username: string) => void): void {
        for (const characters of this.charactersById.values()) {
            for (const character of characters) {
                const projectedPoint = projectOntoCamera(character.getHoverTextPosition(), this.camera);
                if (projectedPoint) {
                    callback(projectedPoint, character.username);
                }
            }
        }
    }

    refreshSize(): void {
        this.width = this.wrapperElement.clientWidth;
        this.height = this.wrapperElement.clientHeight;
        this.camera.aspect = this.width / this.height;
        this.camera.updateProjectionMatrix();
        this.currentRenderer.setSize(this.width, this.height);
    }

    getWidth(): number {
        return this.width;
    }

    getHeight(): number {
        return this.height;
    }

    private add(...objects: ((Object3D | Updatable) & { readonly body?: Body })[]): void {
        for (const object of objects) {
            if (object instanceof Object3D) {
                this.scene.add(object);
            }
            if (this.isUpdatable(object)) {
                this.updatableById.set(object.id, object);
            }
            if (object.body) {
                this.physicsWorld.addBody(object.body);
            }
        }
    }

    private remove(...objects: ((Object3D | Updatable) & { readonly body?: Body })[]): void {
        for (const object of objects) {
            if (object instanceof Object3D) {
                this.scene.remove(object);
            }
            if (this.isUpdatable(object)) {
                this.updatableById.delete(object.id);
            }
            if (object.body) {
                this.physicsWorld.removeBody(object.body);
            }
        }
    }

    private isUpdatable(object: unknown): object is Updatable {
        return !!(object as Updatable).beforePhysics;
    }
}
