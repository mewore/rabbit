import {
    AmbientLight,
    Clock,
    Color,
    Fog,
    HemisphereLight,
    Matrix4,
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
import { AxisHelper, makeGround, makeSkybox, projectOntoCamera } from './util/three-util';
import { Body, Material, Plane, Quaternion, Vec3, World } from 'cannon-es';
import { Settings, addCredit, isReisen } from '../temp-util';
import { AutoFollow } from './util/auto-follow';
import { CannonDebugRenderer } from './util/cannon-debug-renderer';
import { Character } from './character';
import { FixedDistanceOrbitControls } from './util/fixed-distance-orbit-controls';
import { ForestObject } from './forest/forest-object';
import { ForestWall } from './forest/forest-wall';
import { GroundBox } from './util/ground-box';
import { Input } from './util/input';
import { LazyLoadAllocation } from './util/lazy-load-allocation';
import { MapDataMessage } from './entities/messages/map-data-message';
import { MazeMap } from './entities/world/maze-map';
import { Moon } from './moon';
import { PlayerDisconnectMessage } from './entities/messages/player-disconnect-message';
import { PlayerJoinMessage } from './entities/messages/player-join-message';
import { PlayerJoinMutation } from './entities/mutations/player-join-mutation';
import { PlayerUpdateMessage } from './entities/messages/player-update-message';
import { PlayerUpdateMutation } from './entities/mutations/player-update-mutation';
import { SignedBinaryReader } from './entities/data/signed-binary-reader';
import { Updatable } from './util/updatable';
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

const FOV = 60;
// The fog distance has to be adjusted based on the FOV because the fog opacity depends on the distance of the plane
//  perpendicular to the camera intersecting the object insead of on the distance between the camera and the object.
//  Fog is implemented like this in every 3D engine and it's so unrealistic, but I guess it's easier to calculate.
const FOG_END = Math.cos(degToRad(FOV / 2)) * 300;
const FOG_START = FOG_END * 0;

const RESOURCES_PER_FRAME = 100;
const RESOURCES_PER_LAZY_LOAD = 10;

export class GameScene {
    private readonly MAX_DELTA = 0.5;

    private readonly scene: THREE.Scene = new Scene();
    private readonly camera = new PerspectiveCamera(FOV, 1, 1, 1000);
    private currentRenderer: WebGLRenderer = new WebGLRenderer({ antialias: true });
    get renderer(): WebGLRenderer {
        return this.currentRenderer;
    }

    private readonly loadAllocations: LazyLoadAllocation[] = [];
    private readonly updatableById = new Map<number | string, Updatable>();
    private readonly character = new Character('', isReisen());

    private readonly clock = new Clock();
    private readonly elapsedTimeClock = new Clock();

    private readonly charactersById = new Map<number, Character>();

    readonly input = new Input();

    private readonly cameraControls: FixedDistanceOrbitControls;

    private readonly physicsWorld = new World({ gravity: new Vec3(0, -250, 0), allowSleep: true });

    private mapData?: MazeMap;
    readonly forest: ForestObject;
    readonly forestWalls = new ForestWall(this.character.position, this.physicsWorld);

    readonly physicsDebugger = new CannonDebugRenderer(this.scene, this.physicsWorld);

    private width = 0;
    private height = 0;

    constructor(
        private readonly wrapperElement: HTMLElement,
        private readonly webSocket: WebSocket,
        private settings: Settings
    ) {
        this.scene.background = new Color(0x0b051b);
        this.scene.fog = new Fog(this.scene.background, FOG_START, FOG_END);
        this.camera.position.set(-30, 10, -50);
        this.camera.far = FOG_END * 2;
        this.cameraControls = new FixedDistanceOrbitControls(this.input, this.camera, this.character);
        this.cameraControls.minDistance = 10.0;
        this.cameraControls.maxDistance = 100.0;
        this.cameraControls.zoomMultiplier = 1.4;

        // this.renderer.physicallyCorrectLights = true;

        this.camera.rotation.reorder('YXZ');

        this.forest = new ForestObject(
            this.input,
            this.settings.plantsReceiveShadows,
            this.settings.plantVisibility,
            this.camera
        );
        this.loadAllocations.push(this.forest.cellLazyLoad);
        this.forestWalls.padding = this.settings.forestWallActiveRadius;
        this.cameraControls.offset = new Vector3(0, 20, 0);

        makeSkybox().then((skybox) => (this.scene.background = skybox));

        this.add(this.character);
        this.wrapperElement.appendChild(this.currentRenderer.domElement);
        this.currentRenderer.setPixelRatio(window.devicePixelRatio * this.settings.quality);
        this.currentRenderer.shadowMap.enabled = this.settings.shadows;

        const centralLight = new PointLight(0xffdd44, 1, 100.0, 0.9);
        centralLight.position.set(0, 10, 0);
        centralLight.castShadow = true;
        const lightSphere = new Mesh(new SphereBufferGeometry(1, 16, 16), new MeshBasicMaterial({ color: 0xffdd44 }));
        lightSphere.position.copy(centralLight.position);
        lightSphere.material.fog = false;

        this.add(centralLight);
        this.add(lightSphere);

        this.add(new AmbientLight(this.scene.background, 3));
        this.add(new AmbientLight(0x112255, 1));
        this.add(new HemisphereLight(this.scene.background, 0x154f30, 0.5));
        this.physicsWorld.addBody(
            new Body({
                shape: new Plane(),
                quaternion: new Quaternion().setFromEuler(-Math.PI / 2, 0, 0),
                material: new Material({ friction: 0, restitution: 0 }),
            })
        );

        this.add(this.forest);

        const cameraIntersectionObjects: Object3D[] = [];

        const physicsDummyBoxRotation = Math.PI * 0.15;
        const physicsDummyBoxRotationMatrix = new Matrix4().makeRotationY(physicsDummyBoxRotation);
        const physicsDummyBoxAcceleration = -3;
        let physicsDummyBoxSpeed = -25;
        const physicsDummyBoxMovement = new Vector3(1, 0, 0);
        const physicsDummyBoxPos = new Vector3(-30, 0, -30);
        let dummyBoxHeight = 10;
        const dummyBoxHeightAccelerations = [10, 15, 18, -20];
        for (let i = 0; i < 10; i++) {
            const physicsDummyBox = new GroundBox(
                15 + (i === 9 ? 25 : 0),
                dummyBoxHeight,
                physicsDummyBoxPos.x,
                physicsDummyBoxPos.z,
                (i - 0.5) * physicsDummyBoxRotation
            );
            physicsDummyBox.name = 'PhysicsDummyBox' + i;
            this.add(physicsDummyBox);
            cameraIntersectionObjects.push(physicsDummyBox);

            physicsDummyBoxPos.add(
                physicsDummyBoxMovement.multiplyScalar(i === 8 ? physicsDummyBoxSpeed * 1.5 : physicsDummyBoxSpeed)
            );
            physicsDummyBoxMovement
                .multiplyScalar(1 / physicsDummyBoxSpeed)
                .applyMatrix4(physicsDummyBoxRotationMatrix);
            physicsDummyBoxSpeed += physicsDummyBoxAcceleration;
            dummyBoxHeight += dummyBoxHeightAccelerations[i % dummyBoxHeightAccelerations.length];
        }

        this.cameraControls.intersectionObjects = cameraIntersectionObjects;

        this.add(new AxisHelper());

        const moon = new Moon(200);
        moon.target = this.character;
        this.add(moon);

        this.add(new AutoFollow(moon, this.character));
        this.add(this.cameraControls);

        this.physicsDebugger.active = this.settings.debugPhysics;
        this.add(this.physicsDebugger);

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

    get physicsBodyCount(): number {
        return this.physicsWorld.bodies.length;
    }

    get activeForestWallBodyCount(): number {
        return this.forestWalls.activeBodyCount;
    }

    get totalForestWallBodyCount(): number {
        return this.forestWalls.totalBodyCount;
    }

    get time(): number {
        return this.elapsedTimeClock.getElapsedTime();
    }

    applySettings(newSettings: Settings): void {
        let shouldRefreshSize = false;
        if (newSettings.shadows !== this.currentRenderer.shadowMap.enabled) {
            this.wrapperElement.removeChild(this.currentRenderer.domElement);
            this.currentRenderer.forceContextLoss();
            this.currentRenderer.dispose();

            this.currentRenderer = new WebGLRenderer({ antialias: true });
            this.wrapperElement.appendChild(this.currentRenderer.domElement);
            this.currentRenderer.shadowMap.enabled = newSettings.shadows;
            shouldRefreshSize = true;
        }
        if (newSettings.quality !== this.settings.quality) {
            this.currentRenderer.setPixelRatio(window.devicePixelRatio * newSettings.quality);
            shouldRefreshSize = true;
        }
        this.forest.setReceiveShadow(newSettings.plantsReceiveShadows);
        this.forest.visiblePlants = newSettings.plantVisibility;
        this.forestWalls.padding = newSettings.forestWallActiveRadius;
        this.physicsDebugger.active = newSettings.debugPhysics;

        if (shouldRefreshSize) {
            this.refreshSize();
        }

        this.settings = { ...newSettings };
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
        const character = this.getOrCreatePlayerCharacter(
            message.player.id,
            message.player.username,
            message.player.isReisen
        );
        character.setState(message.player.state);
    }

    private onPlayerUpdate(reader: SignedBinaryReader): void {
        const message = PlayerUpdateMessage.decodeFromBinary(reader);
        const character = this.getOrCreatePlayerCharacter(message.playerId, 'Unknown', undefined);
        character.setState(message.newState);
    }

    private onPlayerDisconnected(reader: SignedBinaryReader): void {
        const message = PlayerDisconnectMessage.decodeFromBinary(reader);
        const character = this.charactersById.get(message.playerId);
        if (!character) {
            return;
        }
        this.remove(character);
        this.charactersById.delete(message.playerId);
    }

    private onForestData(reader: SignedBinaryReader): void {
        const message = MapDataMessage.decodeFromBinary(reader);
        this.mapData = message.map;
        this.forest.setMapData(message.map);

        const ground = makeGround(message.map.width, message.map.depth);
        this.add(ground);
        this.cameraControls.intersectionObjects.push(ground);

        this.forestWalls.generate(message.map);
        this.cameraControls.intersectionObjects.push(this.forestWalls);
        this.add(this.forestWalls);
    }

    private getOrCreatePlayerCharacter(playerId: number, username: string, isReisen: boolean | undefined): Character {
        const existingCharacter = this.charactersById.get(playerId);
        if (existingCharacter) {
            return existingCharacter;
        }
        const newCharacter = new Character(username, isReisen);
        this.add(newCharacter);
        this.charactersById.set(playerId, newCharacter);
        return newCharacter;
    }

    animate(): void {
        this.requestMovement(this.input.movementRight, this.input.movementForwards);
        const now = this.time;
        const delta = Math.min(this.clock.getDelta(), this.MAX_DELTA);
        if (this.character.visible && this.input.wantsToJump) {
            this.character.requestJump(now);
        }

        let resources = RESOURCES_PER_FRAME;
        for (const lazyLoad of this.loadAllocations) {
            resources -= lazyLoad.allocateUpTo(RESOURCES_PER_LAZY_LOAD, resources);
        }

        if (this.mapData) {
            for (const character of this.charactersById.values()) {
                this.mapData.wrapTowards(character.position, this.character.position);
            }
        }
        if (delta > 0) {
            for (const updatable of this.updatableById.values()) {
                updatable.beforePhysics(delta, now);
            }

            this.physicsWorld.step(delta, delta);
            for (const updatable of this.updatableById.values()) {
                updatable.afterPhysics(delta, now);
            }
            if (this.mapData) {
                this.character.position.x = this.mapData.wrapX(this.character.position.x);
                this.character.position.z = this.mapData.wrapZ(this.character.position.z);
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
        for (const character of this.charactersById.values()) {
            const projectedPoint = projectOntoCamera(character.getHoverTextPosition(), this.camera);
            if (projectedPoint) {
                callback(projectedPoint, character.username);
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

    private add(
        ...objects: ((Object3D | Updatable) & { readonly body?: Body; readonly bodies?: ReadonlyArray<Body> })[]
    ): void {
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
            if (object.bodies) {
                for (const body of object.bodies) {
                    this.physicsWorld.addBody(body);
                }
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
