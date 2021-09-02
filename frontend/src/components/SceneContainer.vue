<template>
    <div ref="sceneWrapper"></div>
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
}
</script>

<style scoped lang="scss"></style>
