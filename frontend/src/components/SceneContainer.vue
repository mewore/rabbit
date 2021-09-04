<template>
    <div
        ref="sceneWrapper"
        @keydown="onKeyDown($event)"
        @keyup="onKeyUp($event)"
    ></div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import { GameScene } from '../game/game-scene';

@Options({})
export default class SceneContainer extends Vue {
    private scene?: GameScene;

    mounted(): void {
        this.scene = new GameScene(this.$refs.sceneWrapper as HTMLElement);
    }

    beforeUnmount(): void {
        this.scene?.stopRunning();
    }

    onKeyDown(event: KeyboardEvent): void {
        this.setKeyValue(event.code, true);
    }

    onKeyUp(event: KeyboardEvent): void {
        this.setKeyValue(event.code, false);
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

<style scoped lang="scss"></style>
