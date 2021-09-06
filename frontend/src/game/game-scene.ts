import {
    AmbientLight,
    BoxGeometry,
    Clock,
    Color,
    Fog,
    MOUSE,
    Mesh,
    MeshLambertMaterial,
    PerspectiveCamera,
    Scene,
    Vector3,
    WebGLRenderer,
    sRGBEncoding,
} from 'three';
import { AxisHelper, makeGround, projectOntoCamera } from './three-util';
import { AutoFollow } from './auto-follow';
import { Character } from './character';
import { OrbitControls } from '@three-ts/orbit-controls';
import { PlayerConnectEvent } from './entities/events/player-connect-event';
import { PlayerDisconnectEvent } from './entities/events/player-disconnect-event';
import { PlayerSetUpEvent } from './entities/events/player-set-up-event';
import { PlayerSetUpMutation } from './entities/mutations/player-set-up-mutation';
import { PlayerUpdateEvent } from './entities/events/player-update-event';
import { PlayerUpdateMutation } from './entities/mutations/player-update-mutation';
import { SignedBinaryReader } from './entities/data/signed-binary-reader';
import { Sun } from './sun';
import { Updatable } from './updatable';
import { isReisen } from '../temp-util';

enum EventType {
    CONNECT,
    SET_UP,
    UPDATE,
    DISCONNECT,
}

export class GameScene {
    private readonly MAX_DELTA = 0.5;

    private readonly scene: THREE.Scene = new Scene();
    private readonly camera = new PerspectiveCamera(80, 1, 1, 10000);
    private readonly renderer: THREE.WebGLRenderer = new WebGLRenderer({ antialias: true });

    private readonly toUpdate: Updatable[];
    private readonly character = new Character('', isReisen());

    private readonly clock = new Clock();

    private readonly characterById = new Map<number, Character>();

    readonly inputs = {
        up: false,
        down: false,
        left: false,
        right: false,
    };

    constructor(private readonly wrapperElement: HTMLElement, private readonly webSocket: WebSocket) {
        this.scene.background = new Color(0xcce0ff);
        this.scene.fog = new Fog(0xcce0ff, 500, 8000);

        this.camera.position.set(-100, 50, -200);
        this.camera.position.multiplyScalar(2);

        this.scene.add(this.character);
        this.wrapperElement.tabIndex = 0;
        this.wrapperElement.focus();
        this.wrapperElement.appendChild(this.renderer.domElement);
        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.outputEncoding = sRGBEncoding;
        this.renderer.shadowMap.enabled = true;

        this.scene.add(new AmbientLight(0x666666));
        this.scene.add(makeGround());

        const cameraControls = new OrbitControls(this.camera, this.renderer.domElement);
        cameraControls.minPolarAngle = Math.PI * 0.1;
        cameraControls.maxPolarAngle = Math.PI * 0.4;
        cameraControls.minDistance = 50;
        cameraControls.maxDistance = 1000;
        cameraControls.enablePan = false;
        cameraControls.enableDamping = true;
        cameraControls.dampingFactor = 0.1;
        cameraControls.mouseButtons = {
            ZOOM: MOUSE.MIDDLE,
            ORBIT: MOUSE.RIGHT,
            PAN: MOUSE.LEFT,
        };

        cameraControls.target = this.character.position;

        const shadowDummyBox = new Mesh(new BoxGeometry(200, 1000, 200), new MeshLambertMaterial({ color: 0x666666 }));
        shadowDummyBox.name = 'ShadowDummyBox';
        shadowDummyBox.receiveShadow = true;
        shadowDummyBox.castShadow = true;
        shadowDummyBox.position.set(300, 500.0, -100);
        this.scene.add(shadowDummyBox);

        const sun = new Sun(50);
        sun.target = this.character;
        this.scene.add(sun);
        this.scene.add(new AxisHelper());
        this.toUpdate = [this.character, cameraControls, new AutoFollow(sun, this.character)];

        this.webSocket.onopen = () => {
            this.webSocket.send(new PlayerSetUpMutation(isReisen()).encodeToBinary());
            this.webSocket.send(new PlayerUpdateMutation(this.character.getState()).encodeToBinary());
        };
        this.webSocket.onmessage = (message: MessageEvent<ArrayBuffer>) => {
            const reader = new SignedBinaryReader(message.data);
            const eventType = reader.readByte();
            switch (eventType) {
                case EventType.CONNECT:
                    return this.onPlayerConnected(reader);
                case EventType.SET_UP:
                    return this.onPlayerSetUp(reader);
                case EventType.UPDATE:
                    return this.onPlayerUpdate(reader);
                case EventType.DISCONNECT:
                    return this.onPlayerDisconnected(reader);
                default:
                    throw new Error('Unrecognized event type: ' + eventType);
            }
        };

        this.refreshSize();
    }

    private onPlayerConnected(reader: SignedBinaryReader): void {
        const event = PlayerConnectEvent.decodeFromBinary(reader);
        const character = this.getOrCreateCharacter(event.player.id, event.player.username, event.player.isReisen);
        character.setState(event.player.state);
    }

    private onPlayerSetUp(reader: SignedBinaryReader): void {
        const event = PlayerSetUpEvent.decodeFromBinary(reader);
        const character = this.getOrCreateCharacter(event.playerId, 'Unknown', undefined);
        character.setUpMesh(event.isReisen);
    }

    private onPlayerUpdate(reader: SignedBinaryReader): void {
        const event = PlayerUpdateEvent.decodeFromBinary(reader);
        const character = this.getOrCreateCharacter(event.playerId, 'Unknown', undefined);
        character.setState(event.newState);
    }

    private onPlayerDisconnected(reader: SignedBinaryReader): void {
        const event = PlayerDisconnectEvent.decodeFromBinary(reader);
        const character = this.characterById.get(event.playerId);
        if (!character) {
            return;
        }
        this.scene.remove(character);
        this.characterById.delete(event.playerId);
        const index = this.toUpdate.indexOf(character);
        if (index < 0) {
            return;
        }
        this.toUpdate[index] = this.toUpdate[this.toUpdate.length - 1];
        this.toUpdate.pop();
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
        this.requestMovement(
            (this.inputs.right ? 1 : 0) - (this.inputs.left ? 1 : 0),
            (this.inputs.down ? 1 : 0) - (this.inputs.up ? 1 : 0)
        );
        const delta = Math.min(this.clock.getDelta(), this.MAX_DELTA);
        for (const updatable of this.toUpdate) {
            updatable.update(delta);
        }
        this.render();

        if (this.webSocket.readyState === WebSocket.OPEN && this.character.hasChanged()) {
            this.webSocket.send(new PlayerUpdateMutation(this.character.getState()).encodeToBinary());
        }
    }

    private render() {
        this.renderer.render(this.scene, this.camera);
    }

    requestMovement(dx: number, dy: number): void {
        if (Math.abs(dx) > 0 || Math.abs(dy) > 0) {
            this.character.move(this.camera, dy, dx);
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
        const [width, height] = [this.getWidth(), this.getHeight()];
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(width, height);
    }

    getWidth(): number {
        return this.wrapperElement.clientWidth;
    }

    getHeight(): number {
        return this.wrapperElement.clientHeight;
    }
}
