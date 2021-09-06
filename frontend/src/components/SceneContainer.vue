<template>
    <div
        class="scene-wrapper"
        ref="sceneWrapper"
        @keydown="onKeyDown($event)"
        @keyup="onKeyUp($event)"
    >
        <span
            v-for="label in labels"
            :key="label"
            class="floating-label"
            v-bind:style="{
                top: label.top + 'px',
                left: label.left + 'px',
                opacity: label.opacity + '%',
            }"
            >{{ label.text }}</span
        >
    </div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import { GameScene } from '../game/game-scene';

interface LabelInfo {
    top: number;
    left: number;
    text: string;
    opacity: number;
}

@Options({})
export default class SceneContainer extends Vue {
    private webSocket?: WebSocket;
    private scene?: GameScene;
    private requestedAnimationFrame?: number;
    labels: LabelInfo[] = [];

    mounted(): void {
        this.webSocket = new WebSocket(
            `ws://${window.location.host}/multiplayer`
        );
        this.webSocket.binaryType = 'arraybuffer';
        this.scene = new GameScene(
            this.$refs.sceneWrapper as HTMLElement,
            this.webSocket
        );
        this.requestedAnimationFrame = requestAnimationFrame(
            this.animate.bind(this)
        );
        window.addEventListener('resize', this.onResize.bind(this));
    }

    private animate(): void {
        this.requestedAnimationFrame = undefined;
        if (!this.scene) {
            return;
        }
        this.scene.animate();
        this.requestedAnimationFrame = requestAnimationFrame(
            this.animate.bind(this)
        );
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
                this.labels.push({
                    left,
                    top,
                    text: username,
                    opacity,
                });
            } else {
                this.labels[labelIndex].left = left;
                this.labels[labelIndex].top = top;
                this.labels[labelIndex].text = username;
                this.labels[labelIndex].opacity = opacity;
            }
            labelIndex++;
        });
        this.labels.splice(labelIndex);
    }

    beforeUnmount(): void {
        this.scene = undefined;
        if (typeof this.requestedAnimationFrame === 'number') {
            cancelAnimationFrame(this.requestedAnimationFrame);
        }
        this.webSocket?.close();
    }

    onKeyDown(event: KeyboardEvent): void {
        this.setKeyValue(event.code, true);
    }

    onKeyUp(event: KeyboardEvent): void {
        this.setKeyValue(event.code, false);
    }

    onResize(): void {
        if (this.scene) {
            this.scene.refreshSize();
        }
    }

    setKeyValue(keyCode: string, newValue: boolean): void {
        if (!this.scene) {
            return;
        }
        switch (keyCode) {
            case 'KeyW':
            case 'ArrowUp':
                this.scene.inputs.up = newValue;
                break;
            case 'KeyA':
            case 'ArrowLeft':
                this.scene.inputs.left = newValue;
                break;
            case 'KeyS':
            case 'ArrowDown':
                this.scene.inputs.down = newValue;
                break;
            case 'KeyD':
            case 'ArrowRight':
                this.scene.inputs.right = newValue;
                break;
        }
    }
}
</script>

<style scoped lang="scss">
.scene-wrapper {
    position: relative;
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
</style>
