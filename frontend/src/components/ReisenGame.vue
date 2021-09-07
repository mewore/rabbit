<template>
    <div
        class="game-container"
        @keydown="onKeyDown($event)"
        @keyup="onKeyUp($event)"
        @blur="onGameBlurred()"
    >
        <div
            class="canvas-wrapper"
            ref="canvasWrapper"
            @keyup="onCanvasKeyUp($event)"
            tabindex="0"
            :style="{
                filter: `blur(${inactivity}vw) `,
                opacity: 1.0 - inactivity * 0.2,
            }"
        >
            <span
                v-for="label in labels"
                :key="label"
                class="floating-label"
                :style="{
                    top: label.top + 'px',
                    left: label.left + 'px',
                    opacity: label.opacity * (1.0 - inactivity) + '%',
                }"
                >{{ label.text }}</span
            >
        </div>
        <Menu
            v-if="menuIsVisible"
            :playing="playing"
            v-on:close="onMenuClosed()"
        />
    </div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import { GameScene } from '../game/game-scene';
import Menu from '@/components/menu/Menu.vue';

interface LabelInfo {
    top: number;
    left: number;
    text: string;
    opacity: number;
}

const ACTIVE_FPS = 60;
const INACTIVE_FPS = 1;
const INACTIVITY_INCREASE_SPEED = 0.05;
const INACTIVITY_DECREASE_SPEED = 0.5;
const RESIZE_INACTIVITY_MULTIPLIER = 0.8;
const INACTIVITY_THRESHOLD = 0.98;
const INACTIVITY_LOW_THRESHOLD = 0.05;

@Options({
    components: {
        Menu,
    },
})
export default class ReisenGame extends Vue {
    private webSocket?: WebSocket;
    private scene?: GameScene;
    private requestedAnimationFrame?: number;
    labels: LabelInfo[] = [];

    inactivity = 1;

    playing = false;
    menuIsVisible = true;

    mounted(): void {
        this.webSocket = new WebSocket(
            `ws://${window.location.host}/multiplayer`
        );
        const canvasWrapper = this.$refs.canvasWrapper as HTMLElement;
        this.webSocket.binaryType = 'arraybuffer';
        this.scene = new GameScene(canvasWrapper, this.webSocket);
        this.requestedAnimationFrame = requestAnimationFrame(
            this.animate.bind(this)
        );
        this.scene.input.active = !this.menuIsVisible;
        if (!this.menuIsVisible) {
            canvasWrapper.focus();
        }
        window.addEventListener('resize', this.onResize.bind(this));
        document.addEventListener('blur', () => this.scene?.input.clear());
        window.addEventListener('blur', () => this.scene?.input.clear());
    }

    onMenuClosed(): void {
        if (this.scene) {
            this.menuIsVisible = false;
            this.scene.input.active = true;
            (this.$refs.canvasWrapper as HTMLElement).focus();
            this.requestAnimationFrame();
            if (!this.playing) {
                this.playing = true;
                this.scene.start();
            }
        }
    }

    private animate(): void {
        this.requestedAnimationFrame = undefined;
        if (!this.scene) {
            return;
        }
        this.scene.animate();
        if (this.menuIsVisible) {
            if (this.inactivity < 1.0) {
                this.inactivity =
                    this.inactivity > INACTIVITY_THRESHOLD
                        ? 1.0
                        : INACTIVITY_INCREASE_SPEED +
                          (1.0 - INACTIVITY_INCREASE_SPEED) * this.inactivity;
            }
            const maxFps =
                this.inactivity * INACTIVE_FPS +
                (1.0 - this.inactivity) * ACTIVE_FPS;
            setTimeout(() => {
                this.requestAnimationFrame();
            }, 1000 / maxFps);
        } else {
            this.inactivity *= 1.0 - INACTIVITY_DECREASE_SPEED;
            if (this.inactivity < INACTIVITY_LOW_THRESHOLD) {
                this.inactivity = 0.0;
            }
            this.requestAnimationFrame();
        }
        const width = this.scene.getWidth();
        const height = this.scene.getHeight();
        let labelIndex = 0;
        this.scene.forEveryPlayerLabel((position, username) => {
            const closenessCoefficient = 1.0 - position.z / 3000.0;
            if (closenessCoefficient < 0.0) {
                return;
            }
            const left = Math.round(position.x * width);
            const top = Math.round(position.y * height);
            const opacity = Math.round(closenessCoefficient * 100);
            if (labelIndex >= this.labels.length) {
                this.labels.push({} as LabelInfo);
            }
            this.labels[labelIndex].left = left;
            this.labels[labelIndex].top = top;
            this.labels[labelIndex].text = username;
            this.labels[labelIndex].opacity = opacity;
            labelIndex++;
        });
        this.labels.splice(labelIndex);
    }

    private requestAnimationFrame(): void {
        if (this.requestedAnimationFrame) {
            cancelAnimationFrame(this.requestedAnimationFrame);
        }
        this.requestedAnimationFrame = requestAnimationFrame(
            this.animate.bind(this)
        );
    }

    beforeUnmount(): void {
        this.scene = undefined;
        if (typeof this.requestedAnimationFrame === 'number') {
            cancelAnimationFrame(this.requestedAnimationFrame);
        }
        this.webSocket?.close();
    }

    onGameBlurred(): void {
        this.scene?.input.clear();
    }

    onKeyDown(event: KeyboardEvent): void {
        this.setKeyValue(event.code, true);
    }

    onKeyUp(event: KeyboardEvent): void {
        this.setKeyValue(event.code, false);
    }

    onCanvasKeyUp(event: KeyboardEvent): void {
        if (event.code === 'Escape') {
            this.menuIsVisible = true;
            if (this.scene) {
                this.scene.input.active = false;
            }
        }
    }

    onResize(): void {
        if (this.scene) {
            this.inactivity *= RESIZE_INACTIVITY_MULTIPLIER;
            this.scene.refreshSize();
        }
    }

    setKeyValue(keyCode: string, isDown: boolean): void {
        if (!this.scene) {
            return;
        }
        this.scene.input.processKey(keyCode, isDown);
    }
}
</script>

<style scoped lang="scss">
.canvas-wrapper {
    position: absolute;
    height: 100vh;
    width: 100vw;
}
.floating-label {
    position: absolute;
    font-weight: bold;
    user-select: none;
    pointer-events: none;
    font-family: sans-serif;
    min-width: 6em;
    margin-left: -3em;
    height: 1.2em;
    margin-top: -0.6em;
    line-height: 1.2em;
    background: black;
    padding: 5px;
    border-radius: 10px;
    color: white;
    text-align: center;
}
.game-container {
    width: 100%;
    height: 100%;
    position: absolute;
}
</style>
