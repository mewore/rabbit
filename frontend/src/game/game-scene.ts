import Ammo from 'ammo.js';
import {
    AdditiveBlending,
    AddOperation,
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
import { degToRad } from 'three/src/math/MathUtils';

import { Settings } from '@/settings';

import { addCredit, isReisen } from '../temp-util';
import { Character } from './character';
import { AmmoDebugRenderer } from './debug/ammo-debug-renderer';
import { FrameAnalysis } from './debug/frame-analysis';
import { BinaryEntity } from './entities/binary-entity';
import { SignedBinaryReader } from './entities/data/signed-binary-reader';
import { HeartbeatRequest } from './entities/messages/heartbeat-request';
import { MapDataMessage } from './entities/messages/map-data-message';
import { PlayerDisconnectMessage } from './entities/messages/player-disconnect-message';
import { PlayerJoinMessage } from './entities/messages/player-join-message';
import { WorldUpdateMessage } from './entities/messages/world-update-message';
import { HeartbeatResponse } from './entities/mutations/heartbeat-response';
import { PlayerInputMutation } from './entities/mutations/player-input-mutation';
import { PlayerJoinMutation } from './entities/mutations/player-join-mutation';
import { PlayerInput } from './entities/player/player-input';
import { MazeMap } from './entities/world/maze-map';
import { ForestObject } from './forest/forest-object';
import { ForestWall } from './forest/forest-wall';
import { Moon } from './moon';
import { BulletCollisionFlags } from './physics/bullet-collision-flags';
import { RigidBodyFollow } from './physics/rigid-body-mesh';
import { WorldSimulation } from './simulation/world-simulation';
import { AutoFollow } from './util/auto-follow';
import { FixedDistanceOrbitControls } from './util/fixed-distance-orbit-controls';
import { Input } from './util/input';
import { LazyLoadAllocation } from './util/lazy-load-allocation';
import { PhysicsAware } from './util/physics-aware';
import { PhysicsDummyBox } from './util/physics-dummy-box';
import { RenderAware } from './util/render-aware';
import { AxisHelper, makeGround, makeSkybox, projectOntoCamera } from './util/three-util';

enum MessageType {
    JOIN,
    FOREST_DATA,
    UPDATE,
    DISCONNECT,
    HEARTBEAT_REQUEST,
}

addCredit({
    thing: { text: 'Three.js', url: 'https://threejs.org/' },
    author: { text: 'Mr.doob', url: 'https://github.com/mrdoob' },
});

const FOV = 60;
// The fog distance has to be adjusted based on the FOV because the fog opacity depends on the distance of the plane
//  perpendicular to the camera intersecting the object insead of on the distance between the camera and the object.
//  Fog is implemented like this in every 3D engine and it's so unrealistic, but I guess it's easier to calculate.
const FOG_END = Math.cos(degToRad(FOV / 2)) * 300;
const FOG_START = FOG_END * 0;

const RESOURCES_PER_FRAME = 100;
const RESOURCES_PER_LAZY_LOAD = 10;

const GROUND_HALF_THICKNESS = 100;

const DUMMY_SPHERE_GEOMETRY = new SphereBufferGeometry(10, 16, 16);
const DUMMY_SPHERE_MATERIAL = new MeshBasicMaterial({
    color: 0x22ffff,
    transparent: true,
    opacity: 0.5,
    fog: false,
    blending: AdditiveBlending,
    combine: AddOperation,
});

type GameObject = (Object3D | PhysicsAware | RenderAware) & {
    readonly body?: Ammo.btCollisionObject;
    readonly collisionFilterMask?: number;
    readonly collisionFilterGroup?: number;
    readonly bodies?: ReadonlyArray<Ammo.btCollisionObject>;
    readonly actionInterface?: Ammo.btActionInterface;
};

export class GameScene {
    private readonly MAX_DELTA = 0.5;

    private readonly scene: THREE.Scene = new Scene();
    private readonly camera = new PerspectiveCamera(FOV, 1, 1, 1000);
    private currentRenderer: WebGLRenderer = new WebGLRenderer({ antialias: true });
    get renderer(): WebGLRenderer {
        return this.currentRenderer;
    }

    private readonly loadAllocations: LazyLoadAllocation[] = [];
    private readonly physicsAwareById = new Map<number | string, PhysicsAware>();
    private readonly renderAwareById = new Map<number | string, RenderAware>();
    private readonly character: Character;

    private readonly clock = new Clock();
    private readonly elapsedTimeClock = new Clock();

    private readonly characterById = new Map<number, Character>();

    private lastSentInputId = -1;
    readonly input = new Input();

    private readonly cameraControls: FixedDistanceOrbitControls;

    private readonly physicsWorld;

    private mapData?: MazeMap;
    readonly forest: ForestObject;
    readonly forestWalls: ForestWall;

    private readonly physicsDebugger: AmmoDebugRenderer;

    private readonly simulation: WorldSimulation;

    private readonly sphereShape = new Ammo.btSphereShape(10);

    private worldUpdateToApply?: WorldUpdateMessage;

    private width = 0;
    private height = 0;

    constructor(
        private readonly wrapperElement: HTMLElement,
        private readonly webSocket: WebSocket,
        private settings: Settings,
        private readonly frameAnalysis: FrameAnalysis
    ) {
        // Set up the physics world
        this.physicsDebugger = new AmmoDebugRenderer(this.frameAnalysis);
        const configuration = new Ammo.btDefaultCollisionConfiguration();
        this.physicsWorld = new Ammo.btDiscreteDynamicsWorld(
            new Ammo.btCollisionDispatcher(configuration),
            new Ammo.btDbvtBroadphase(),
            new Ammo.btSequentialImpulseConstraintSolver(),
            configuration
        );
        this.physicsWorld.getPairCache().setInternalGhostPairCallback(new Ammo.btGhostPairCallback());
        this.physicsWorld.setGravity(new Ammo.btVector3(0, -250, 0));

        this.character = new Character(this.physicsWorld, '', isReisen(), true);
        this.forestWalls = new ForestWall(this.character.position, this.physicsWorld);

        this.simulation = new WorldSimulation(
            this.frameAnalysis,
            this.physicsWorld,
            this.physicsAwareById,
            this.character,
            this.characterById,
            this.createSphere.bind(this)
        );
        // Setup of the physics world done

        this.scene.background = new Color(0x0b051b);
        this.scene.fog = new Fog(this.scene.background, FOG_START, FOG_END);
        this.camera.position.set(-30, 10, -50);
        this.camera.far = FOG_END * 2;
        this.cameraControls = new FixedDistanceOrbitControls(
            this.input,
            this.camera,
            this.character,
            this.physicsWorld
        );
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
        this.loadAllocations.push(this.forestWalls.wallLazyLoad);
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

        this.add(this.forest);

        this.add(new AxisHelper());

        const moon = new Moon(200);
        moon.target = this.character;
        this.add(moon);

        this.add(new AutoFollow(moon, this.character));
        this.add(this.cameraControls);

        this.add(this.physicsDebugger);
        this.physicsWorld.setDebugDrawer(this.physicsDebugger.drawer);
        this.physicsDebugger.visible = this.settings.debugPhysics;

        this.webSocket.onmessage = (message: MessageEvent<ArrayBuffer>) => {
            if (this.settings.artificialLatency >= 5) {
                setTimeout(() => this.receiveData(message), this.settings.artificialLatency - 5);
            } else {
                this.receiveData(message);
            }
        };

        this.refreshSize();
        this.character.visible = false;
    }

    get physicsBodyCount(): number {
        return 0;
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

    createSphere(): Ammo.btRigidBody {
        const sphereConstructionInfo = new Ammo.btRigidBodyConstructionInfo(
            1,
            new Ammo.btDefaultMotionState(),
            this.sphereShape
        );
        const sphere = new Ammo.btRigidBody(sphereConstructionInfo);
        sphere.setFriction(0.5);
        sphere.setRestitution(1);

        const sphereMesh = new Mesh(DUMMY_SPHERE_GEOMETRY, DUMMY_SPHERE_MATERIAL);
        const sphereLight = new PointLight(DUMMY_SPHERE_MATERIAL.color, 2, 50, 0.9);
        sphereMesh.attach(sphereLight);
        this.add(sphereMesh, sphereLight, new RigidBodyFollow(sphere, sphereMesh, sphereLight));
        return sphere;
    }

    applySettings(newSettings: Settings): void {
        let shouldRefreshSize = false;
        if (newSettings.shadows !== this.currentRenderer.shadowMap.enabled) {
            this.wrapperElement.removeChild(this.currentRenderer.domElement);
            this.currentRenderer.forceContextLoss();
            this.currentRenderer.dispose();

            this.currentRenderer = new WebGLRenderer({ antialias: true });
            this.currentRenderer.setPixelRatio(window.devicePixelRatio * newSettings.quality);
            this.currentRenderer.shadowMap.enabled = newSettings.shadows;
            this.wrapperElement.appendChild(this.currentRenderer.domElement);
            shouldRefreshSize = true;
        }
        if (newSettings.quality !== this.settings.quality) {
            this.currentRenderer.setPixelRatio(window.devicePixelRatio * newSettings.quality);
            shouldRefreshSize = true;
        }
        this.forest.setReceiveShadow(newSettings.plantsReceiveShadows);
        this.forest.visiblePlants = newSettings.plantVisibility;
        this.forestWalls.padding = newSettings.forestWallActiveRadius;
        this.physicsDebugger.visible = newSettings.debugPhysics;

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
        this.renderAwareById.set(this.character.id, this.character);
        this.physicsAwareById.set(this.character.id, this.character);
        if (this.webSocket.readyState === WebSocket.OPEN) {
            this.sendInitialPlayerInfo();
        } else {
            this.webSocket.onopen = () => this.sendInitialPlayerInfo();
        }
    }

    private sendInitialPlayerInfo(): void {
        this.sendData(new PlayerJoinMutation(isReisen()));
    }

    private sendData(data: BinaryEntity): void {
        if (this.settings.artificialLatency >= 5) {
            setTimeout(() => this.webSocket.send(data.encodeToBinary()), this.settings.artificialLatency - 5);
        } else {
            this.webSocket.send(data.encodeToBinary());
        }
    }

    private receiveData(message: MessageEvent<ArrayBuffer>): void {
        const reader = new SignedBinaryReader(message.data);
        const messageType = reader.readByte();
        switch (messageType) {
            case MessageType.JOIN:
                return this.onPlayerJoined(reader);
            case MessageType.UPDATE:
                return this.onWorldUpdate(reader);
            case MessageType.DISCONNECT:
                return this.onPlayerDisconnected(reader);
            case MessageType.FOREST_DATA:
                return this.onForestData(reader);
            case MessageType.HEARTBEAT_REQUEST:
                return this.onHeartbeat(reader);
            default:
                throw new Error('Unrecognized message type: ' + messageType);
        }
    }

    private onPlayerJoined(reader: SignedBinaryReader): void {
        const message = PlayerJoinMessage.decodeFromBinary(reader);
        if (message.isSelf) {
            const existing = this.characterById.get(message.playerId);
            if (existing) {
                this.remove(existing);
            }
            this.character.username = message.username;
            this.character.playerId = message.playerId;
            this.characterById.set(message.playerId, this.character);
        }
        this.getOrCreatePlayerCharacter(message.playerId, message.username, message.isReisen);
    }

    private onWorldUpdate(reader: SignedBinaryReader): void {
        this.worldUpdateToApply = WorldUpdateMessage.decodeFromBinary(reader);
        this.simulation.applyUpdateInputs(this.worldUpdateToApply);
    }

    private onPlayerDisconnected(reader: SignedBinaryReader): void {
        const message = PlayerDisconnectMessage.decodeFromBinary(reader);
        const character = this.characterById.get(message.playerId);
        if (!character) {
            return;
        }
        this.remove(character);
        this.characterById.delete(message.playerId);
    }

    private onForestData(reader: SignedBinaryReader): void {
        const message = MapDataMessage.decodeFromBinary(reader);
        this.mapData = message.map;
        this.simulation.map = this.mapData;
        this.forest.setMapData(message.map);

        const ground = makeGround(message.map.width, message.map.depth);
        this.add(ground);

        this.forestWalls.generate(message.map, 50);
        this.add(this.forestWalls);

        const planeShape = new Ammo.btBoxShape(
            new Ammo.btVector3(message.map.width, GROUND_HALF_THICKNESS, message.map.depth)
        );
        const constructionInfo = new Ammo.btRigidBodyConstructionInfo(0, new Ammo.btDefaultMotionState(), planeShape);
        const groundPlane = new Ammo.btRigidBody(constructionInfo);
        groundPlane.getWorldTransform().getOrigin().setY(-GROUND_HALF_THICKNESS);
        groundPlane.setCollisionFlags(BulletCollisionFlags.STATIC_OBJECT);
        groundPlane.setFriction(0.75);
        groundPlane.setRestitution(0.25);
        this.physicsWorld.addRigidBody(groundPlane);

        for (let i = 0; i < message.dummyBoxes.length; i++) {
            const box = message.dummyBoxes[i];
            const physicsDummyBox = new PhysicsDummyBox(box.width, box.height, box.position, box.rotationY);
            if (i === message.dummyBoxes.length - 1) {
                physicsDummyBox.material = PhysicsDummyBox.SHINY_MATERIAL;
            }
            this.add(physicsDummyBox);
        }
    }

    private onHeartbeat(reader: SignedBinaryReader): void {
        const message = HeartbeatRequest.decodeFromBinary(reader);
        this.sendData(new HeartbeatResponse(message.id));
    }

    private getOrCreatePlayerCharacter(playerId: number, username: string, isReisen: boolean | undefined): Character {
        const existingCharacter = this.characterById.get(playerId);
        if (existingCharacter) {
            return existingCharacter;
        }
        const newCharacter = new Character(this.physicsWorld, username, isReisen);
        newCharacter.playerId = playerId;
        this.add(newCharacter);
        this.characterById.set(playerId, newCharacter);
        return newCharacter;
    }

    animate(): void {
        const now = this.time;
        const delta = Math.min(this.clock.getDelta(), this.MAX_DELTA);

        this.character.inputId = this.input.id;
        if (
            this.character.visible &&
            this.character.playerId > -1 &&
            (this.input.id > this.lastSentInputId || this.simulation.shouldResendInput) &&
            this.simulation.hasServerUpdate &&
            this.webSocket.readyState === WebSocket.OPEN
        ) {
            const input = new PlayerInput(
                this.input.id,
                this.simulation.currentFrame,
                PlayerInput.compressInput(this.input),
                this.camera.rotation.y
            );
            if (this.frameAnalysis.analyzing) {
                this.frameAnalysis.addMessage(`Sending input #${input.id} for frame #${input.frameId}: ${input}`);
            }
            this.sendData(new PlayerInputMutation(input));
            this.simulation.shouldResendInput = false;
            this.lastSentInputId = this.input.id;
            this.simulation.acceptInput(input);
        }
        if (this.worldUpdateToApply) {
            this.simulation.applyUpdate(this.worldUpdateToApply, this.time);
            this.worldUpdateToApply = undefined;
        }

        let resources = RESOURCES_PER_FRAME;
        for (const lazyLoad of this.loadAllocations) {
            resources -= lazyLoad.allocateUpTo(RESOURCES_PER_LAZY_LOAD, resources);
        }

        this.simulation.simulateUntil(now);
        if (this.settings.debugPhysics) {
            this.physicsDebugger.clearGeometry();
            this.physicsWorld.debugDrawWorld();
        }

        for (const entity of this.renderAwareById.values()) {
            entity.longBeforeRender(delta, now);
        }
        for (const entity of this.renderAwareById.values()) {
            entity.beforeRender(delta, now);
        }
        this.render();

        if (this.frameAnalysis.analyzing) {
            this.frameAnalysis.captureFrame(this.currentRenderer.domElement, this.simulation.currentFrame);
        }

        this.input.clearMouseDelta();
    }

    private render() {
        this.currentRenderer.render(this.scene, this.camera);
    }

    forEveryPlayerLabel(callback: (position: Vector3, username: string) => void): void {
        for (const character of this.characterById.values()) {
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

    private add(...objects: GameObject[]): void {
        for (const object of objects) {
            if (object instanceof Object3D) {
                this.scene.add(object);
            }
            if (this.isPhysicsAware(object)) {
                this.physicsAwareById.set(object.id, object);
            }
            if (this.isRenderAware(object)) {
                this.renderAwareById.set(object.id, object);
            }
            if (object.body) {
                if (this.isRigidBody(object.body)) {
                    if (object.collisionFilterGroup != null && object.collisionFilterMask != null) {
                        this.physicsWorld.addRigidBody(
                            object.body,
                            object.collisionFilterGroup,
                            object.collisionFilterMask
                        );
                    } else {
                        this.physicsWorld.addRigidBody(object.body);
                    }
                } else {
                    if (object.collisionFilterGroup != null && object.collisionFilterMask != null) {
                        this.physicsWorld.addCollisionObject(
                            object.body,
                            object.collisionFilterGroup,
                            object.collisionFilterMask
                        );
                    } else {
                        this.physicsWorld.addCollisionObject(object.body);
                    }
                }
            }
            if (object.bodies) {
                for (const body of object.bodies) {
                    if (this.isRigidBody(body)) {
                        this.physicsWorld.addRigidBody(body);
                    } else {
                        this.physicsWorld.addCollisionObject(body);
                    }
                }
            }
            if (object.actionInterface && object.actionInterface instanceof Ammo.btActionInterface) {
                this.physicsWorld.addAction(object.actionInterface);
            }
        }
    }

    private remove(...objects: GameObject[]): void {
        for (const object of objects) {
            if (object instanceof Object3D) {
                this.scene.remove(object);
            }
            if (this.isPhysicsAware(object)) {
                this.physicsAwareById.delete(object.id);
            }
            if (this.isRenderAware(object)) {
                this.renderAwareById.delete(object.id);
            }
            if (object.body) {
                if (this.isRigidBody(object.body)) {
                    this.physicsWorld.removeRigidBody(object.body);
                } else {
                    this.physicsWorld.removeCollisionObject(object.body);
                }
            }
            if (object.actionInterface) {
                this.physicsWorld.removeAction(object.actionInterface);
            }
        }
    }

    private isRigidBody(body: unknown): body is Ammo.btRigidBody {
        return !!(body as Ammo.btRigidBody).getLinearVelocity;
    }

    private isPhysicsAware(object: unknown): object is PhysicsAware {
        return !!(object as PhysicsAware).beforePhysics && !!(object as PhysicsAware).afterPhysics;
    }

    private isRenderAware(object: unknown): object is RenderAware {
        return !!(object as RenderAware).beforeRender;
    }
}
