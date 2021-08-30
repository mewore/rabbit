<template>
    <div ref="sceneWrapper"></div>
</template>

<script lang="ts">
import { Options, Vue } from "vue-class-component";
import { GameScene } from "../game/game-scene";
import { GameRuntime } from "../game/game-runtime";

@Options({})
export default class SceneContainer extends Vue {
    private game?: GameRuntime;

    mounted(): void {
        this.game = new GameScene(
            this.$refs.sceneWrapper as HTMLElement
        ).runtime;
    }

    beforeUnmount(): void {
        this.game?.stopRunning();
    }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
h3 {
    margin: 40px 0 0;
}
ul {
    list-style-type: none;
    padding: 0;
}
li {
    display: inline-block;
    margin: 0 10px;
}
a {
    color: #42b983;
}
</style>
