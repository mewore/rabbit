<template>
    <ReisenGame />
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import ReisenGame from './components/ReisenGame.vue';

@Options({
    components: {
        ReisenGame,
    },
})
export default class App extends Vue {}
</script>

<style lang="scss">
body {
    overflow: hidden;
    margin: 0;
    background: black;
}
a {
    text-decoration: none;
    &:hover {
        text-decoration: underline;
    }
}
</style>
