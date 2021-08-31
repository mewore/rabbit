<template>
    <h1 id="title-header">{{ title }}</h1>
    <SceneContainer />
    <Footer />
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import { SiteType, getSiteType } from './temp-util';
import Footer from './components/Footer.vue';
import SceneContainer from './components/SceneContainer.vue';

@Options({
    components: {
        SceneContainer,
        Footer,
    },
})
export default class App extends Vue {
    title = '';

    mounted(): void {
        switch (getSiteType()) {
            case SiteType.REISEN:
                this.title = 'Reisen';
                break;
            case SiteType.TEWI:
                this.title =
                    "Tewi (actually just a carrot because I don't have a model for her :c)";
                break;
            default:
                return;
        }
        document.title = this.title;
    }
}
</script>

<style lang="scss">
#title-header {
    text-align: center;
    margin: 1em auto;
    width: 100%;
    position: absolute;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    user-select: none;
    pointer-events: none;
    color: rgba(100, 0, 200, 0.5);
}
body {
    overflow: hidden;
    margin: 0;
}
</style>
