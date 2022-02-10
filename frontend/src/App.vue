<template>
    <ReisenGame @darkUiSetting="onDarkUiSetting" />
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import ReisenGame from './components/ReisenGame.vue';
import { useQuasar } from 'quasar';

@Options({
    components: {
        ReisenGame,
    },
})
export default class App extends Vue {
    readonly quasar = useQuasar();

    mounted(): void {
        this.quasar.dark.set(true);
    }

    onDarkUiSetting(newSetting: boolean): void {
        this.quasar.dark.set(newSetting);
    }
}
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
