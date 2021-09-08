import {
    AmbientLight,
    BoxGeometry,
    Clock,
    Color,
    Fog,
    Mesh,
    MeshLambertMaterial,
    PerspectiveCamera,
    Scene,
    Vector3,
    WebGLRenderer,
    sRGBEncoding,
} from 'three';
import { AxisHelper, makeGround, projectOntoCamera } from './three-util';
import { addCredit, isReisen } from '../temp-util';
import { AutoFollow } from './auto-follow';
import { Character } from './character';
import { Input } from './input';
import { PlayerDisconnectEvent } from './entities/events/player-disconnect-event';
import { PlayerJoinEvent } from './entities/events/player-join-event';
import { PlayerJoinMutation } from './entities/mutations/player-join-mutation';
import { PlayerUpdateEvent } from './entities/events/player-update-event';
import { PlayerUpdateMutation } from './entities/mutations/player-update-mutation';
import { RigidOrbitControls } from './rigid-orbit-controls';
import { SignedBinaryReader } from './entities/data/signed-binary-reader';
import { Sun } from './sun';
import { Updatable } from './updatable';

enum EventType {
    CONNECT,
    SET_UP,
    UPDATE,
    DISCONNECT,
}

addCredit({
    thing: { text: 'Three.js', url: 'https://threejs.org/' },
    author: { text: 'Mr.doob', url: 'https://github.com/mrdoob' },
});

export class GameScene {
    private readonly MAX_DELTA = 0.5;

    private readonly scene: THREE.Scene = new Scene();
    private readonly camera = new PerspectiveCamera(80, 1, 1, 1000);
    private readonly renderer: THREE.WebGLRenderer = new WebGLRenderer({ antialias: true });

    private readonly toUpdate: Updatable[];
    private readonly character = new Character('', isReisen());

    private readonly clock = new Clock();

    private readonly characterById = new Map<number, Character>();

    readonly input = new Input();

    constructor(private readonly wrapperElement: HTMLElement, private readonly webSocket: WebSocket) {
        this.scene.background = new Color(0xcce0ff);
        this.scene.fog = new Fog(0xcce0ff, 50, 800);
        1;
        this.camera.position.set(-20, 10, -40);

        this.scene.add(this.character);
        this.wrapperElement.appendChild(this.renderer.domElement);
        this.renderer.setPixelRatio(window.devicePixelRatio);
        this.renderer.outputEncoding = sRGBEncoding;
        this.renderer.shadowMap.enabled = true;

        this.scene.add(new AmbientLight(0x666666));
        const ground = makeGround();
        this.scene.add(ground);

        const shadowDummyBox = new Mesh(new BoxGeometry(20, 100, 20), new MeshLambertMaterial({ color: 0x666666 }));
        shadowDummyBox.name = 'ShadowDummyBox';
        shadowDummyBox.receiveShadow = true;
        shadowDummyBox.castShadow = true;
        shadowDummyBox.position.set(30, 50, -10);
        this.scene.add(shadowDummyBox);

        const cameraControls = new RigidOrbitControls(this.input, this.camera, this.character);
        cameraControls.intersectionObjects = [ground, shadowDummyBox];

        const sun = new Sun(50);
        sun.target = this.character;
        this.scene.add(sun);
        this.scene.add(new AxisHelper());
        this.toUpdate = [cameraControls, new AutoFollow(sun, this.character)];

        this.webSocket.onmessage = (message: MessageEvent<ArrayBuffer>) => {
            const reader = new SignedBinaryReader(message.data);
            const eventType = reader.readByte();
            switch (eventType) {
                case EventType.CONNECT:
                    return this.onPlayerConnected(reader);
                case EventType.SET_UP:
                    return this.onPlayerJoined(reader);
                case EventType.UPDATE:
                    return this.onPlayerUpdate(reader);
                case EventType.DISCONNECT:
                    return this.onPlayerDisconnected(reader);
                default:
                    throw new Error('Unrecognized event type: ' + eventType);
            }
        };

        this.refreshSize();
        this.character.visible = false;
    }

    start(): void {
        if (this.character.visible) {
            return;
        }
        this.character.visible = true;
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

    private onPlayerConnected(reader: SignedBinaryReader): void {
        const event = PlayerJoinEvent.decodeFromBinary(reader);
        const character = this.getOrCreateCharacter(event.player.id, event.player.username, event.player.isReisen);
        character.setState(event.player.state);
    }

    private onPlayerJoined(reader: SignedBinaryReader): void {
        const event = PlayerJoinEvent.decodeFromBinary(reader);
        this.getOrCreateCharacter(event.player.id, event.player.username, event.player.isReisen);
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
        this.requestMovement(this.input.movementRight, this.input.movementForwards);
        const delta = Math.min(this.clock.getDelta(), this.MAX_DELTA);
        for (const updatable of this.toUpdate) {
            updatable.update(delta);
        }
        this.render();

        if (this.character.visible && this.webSocket.readyState === WebSocket.OPEN && this.character.hasChanged()) {
            this.webSocket.send(new PlayerUpdateMutation(this.character.getState()).encodeToBinary());
        }
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
