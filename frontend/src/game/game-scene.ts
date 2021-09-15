import {
    AmbientLight,
    BoxBufferGeometry,
    Clock,
    Color,
    Fog,
    HemisphereLight,
    Mesh,
    MeshLambertMaterial,
    PerspectiveCamera,
    Scene,
    Vector3,
    WebGLRenderer,
} from 'three';
import { AxisHelper, makeGround, makeSkybox, projectOntoCamera, wrap } from './three-util';
import { addCredit, isReisen } from '../temp-util';
import { AutoFollow } from './auto-follow';
import { Character } from './character';
import { FixedDistanceOrbitControls } from './fixed-distance-orbit-controls';
import { ForestDataMessage } from './entities/messages/forest-data-message';
import { ForestObject } from './forest/forest-object';
import { Input } from './input';
import { Moon } from './moon';
import { PlayerDisconnectMessage } from './entities/messages/player-disconnect-message';
import { PlayerJoinMessage } from './entities/messages/player-join-message';
import { PlayerJoinMutation } from './entities/mutations/player-join-mutation';
import { PlayerUpdateMessage } from './entities/messages/player-update-message';
import { PlayerUpdateMutation } from './entities/mutations/player-update-mutation';
import { SignedBinaryReader } from './entities/data/signed-binary-reader';
import { Updatable } from './updatable';
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

const GROUND_TEXTURE_SCALE = 1 / 64;
const ASSUMED_GROUND_TEXTURE_WIDTH = 512 * 2 * GROUND_TEXTURE_SCALE;
const ASSUMED_GROUND_TEXTURE_HEIGHT = 880 * 2 * GROUND_TEXTURE_SCALE;
const DESIRED_WORLD_SIZE = 500;
const WORLD_WIDTH = Math.round(DESIRED_WORLD_SIZE / ASSUMED_GROUND_TEXTURE_WIDTH) * ASSUMED_GROUND_TEXTURE_WIDTH;
const WORLD_DEPTH = Math.round(DESIRED_WORLD_SIZE / ASSUMED_GROUND_TEXTURE_HEIGHT) * ASSUMED_GROUND_TEXTURE_HEIGHT;

const MIN_X = -WORLD_WIDTH / 2;
const MAX_X = WORLD_WIDTH / 2;
const MIN_Z = -WORLD_DEPTH / 2;
const MAX_Z = WORLD_DEPTH / 2;

const FOV = 60;
// The fog distance has to be adjusted based on the FOV because the fog opacity depends on the distance of the plane
//  perpendicular to the camera intersecting the object insead of on the distance between the camera and the object.
//  Fog is implemented like this in every damn 3D engine and it's so unrealistic, but I guess it's easier to calculate.
const FOG_END = Math.cos(degToRad(FOV / 2)) * (Math.min(WORLD_WIDTH, WORLD_DEPTH) * 0.5);
const FOG_START = FOG_END * 0;

export class GameScene {
    private readonly MAX_DELTA = 0.5;

    private readonly scene: THREE.Scene = new Scene();
    private readonly camera = new PerspectiveCamera(FOV, 1, 1, 1000);
    readonly renderer: WebGLRenderer = new WebGLRenderer({ antialias: true });

    private readonly toUpdate: Updatable[] = [];
    private readonly character = new Character('', isReisen());

    private readonly clock = new Clock();
    private readonly elapsedTimeClock = new Clock();

    private readonly characterById = new Map<number, Character>();

    readonly input = new Input();

    private readonly cameraControls: FixedDistanceOrbitControls;

    private readonly forest = new ForestObject(WORLD_WIDTH, WORLD_DEPTH);

    private width = 0;
    private height = 0;

    constructor(private readonly wrapperElement: HTMLElement, private readonly webSocket: WebSocket) {
        this.scene.background = new Color(0x0b051b);
        this.scene.fog = new Fog(this.scene.background, FOG_START, FOG_END);
        this.camera.position.set(-30, 10, -50);
        this.camera.far = FOG_END * 2;
        this.cameraControls = new FixedDistanceOrbitControls(this.input, this.camera, this.character);
        this.camera.rotation.reorder('YXZ');

        makeSkybox().then((skybox) => (this.scene.background = skybox));

        this.scene.add(this.character);
        this.wrapperElement.appendChild(this.renderer.domElement);
        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.shadowMap.enabled = true;

        this.scene.add(new AmbientLight(this.scene.background, 3));
        this.scene.add(new AmbientLight(0x223333, 1));
        this.scene.add(new HemisphereLight(this.scene.background, 0x154f30, 0.5));
        const ground = makeGround();
        this.scene.add(ground);

        this.scene.add(this.forest);

        const shadowDummyBox = new Mesh(
            new BoxBufferGeometry(20, 100, 20),
            new MeshLambertMaterial({ color: 0x666666 })
        );
        shadowDummyBox.name = 'ShadowDummyBox';
        shadowDummyBox.receiveShadow = true;
        shadowDummyBox.castShadow = true;
        shadowDummyBox.position.set(30, 50, -10);
        this.scene.add(shadowDummyBox, new AxisHelper());

        this.cameraControls.intersectionObjects = [ground, shadowDummyBox];

        const moon = new Moon(50);
        moon.target = this.character;
        this.scene.add(moon);

        this.toUpdate = [new AutoFollow(moon, this.character)];

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

    get time(): number {
        return this.elapsedTimeClock.getElapsedTime();
    }

    start(): void {
        if (this.character.visible) {
            return;
        }
        this.character.visible = true;
        this.character.position.y = 300;
        this.toUpdate.push(this.character);
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
        const character = this.getOrCreateCharacter(
            message.player.id,
            message.player.username,
            message.player.isReisen
        );
        character.setState(message.player.state);
    }

    private onPlayerUpdate(reader: SignedBinaryReader): void {
        const message = PlayerUpdateMessage.decodeFromBinary(reader);
        const character = this.getOrCreateCharacter(message.playerId, 'Unknown', undefined);
        character.setState(message.newState);
    }

    private onPlayerDisconnected(reader: SignedBinaryReader): void {
        const message = PlayerDisconnectMessage.decodeFromBinary(reader);
        const character = this.characterById.get(message.playerId);
        if (!character) {
            return;
        }
        this.scene.remove(character);
        this.characterById.delete(message.playerId);
        const index = this.toUpdate.indexOf(character);
        if (index < 0) {
            return;
        }
        this.toUpdate[index] = this.toUpdate[this.toUpdate.length - 1];
        this.toUpdate.pop();
    }

    private onForestData(reader: SignedBinaryReader): void {
        const message = ForestDataMessage.decodeFromBinary(reader);
        this.forest.setForestData(message.forest);
    }

    private getOrCreateCharacter(playerId: number, username: string, isReisen: boolean | undefined): Character {
        const existingCharacter = this.characterById.get(playerId);
        if (existingCharacter) {
            return existingCharacter;
        }
        const newCharacter = new Character(username, isReisen);
        this.scene.add(newCharacter);
        this.toUpdate.push(newCharacter);
        this.characterById.set(playerId, newCharacter);
        return newCharacter;
    }

    animate(): void {
        this.requestMovement(this.input.movementRight, this.input.movementForwards);
        const now = this.time;
        const delta = Math.min(this.clock.getDelta(), this.MAX_DELTA);
        if (this.character.visible && this.input.wantsToJump) {
            this.character.jump(now);
        }
        if (this.character.visible) {
            const characterPosition = this.character.position;
            if (characterPosition.x < MIN_X || characterPosition.x > MAX_X) {
                characterPosition.setX(wrap(characterPosition.x, MIN_X, MAX_X));
            }
            if (characterPosition.z < MIN_Z || characterPosition.z > MAX_Z) {
                characterPosition.setZ(wrap(characterPosition.z, MIN_Z, MAX_Z));
            }
            if (this.webSocket.readyState === WebSocket.OPEN && this.character.hasChanged()) {
                this.webSocket.send(new PlayerUpdateMutation(this.character.getState()).encodeToBinary());
            }
        }
        for (const updatable of this.toUpdate) {
            updatable.update(delta, now);
        }
        this.cameraControls.update(delta);
        this.render();
    }

    private render() {
        this.renderer.render(this.scene, this.camera);
    }

    private requestMovement(side: number, forwards: number): void {
        if (!this.character.visible) {
            return;
        }
        if (Math.abs(side) > 0 || Math.abs(forwards) > 0) {
            this.character.move(this.camera, forwards, side);
        } else {
            this.character.stopMoving();
        }
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
        this.renderer.setSize(this.width, this.height);
    }

    getWidth(): number {
        return this.width;
    }

    getHeight(): number {
        return this.height;
    }
}
