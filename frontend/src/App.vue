<template>
    <h1 id="title-header">{{ title }}</h1>
    <SceneContainer />
    <div id="footer">
        <div class="credits-title">Credits</div>
        <div>
            <span ref="touhouCredit">Touhou</span> by
            <a target="_blank" href="https://en.touhouwiki.net/wiki/ZUN">ZUN</a>
        </div>
        <div></div>
    </div>
</template>

<script lang="ts">
import { Options, Vue } from "vue-class-component";
import SceneContainer from "./components/SceneContainer.vue";
import { getSiteType, SiteType } from "./temp-util";

@Options({
    components: {
        SceneContainer,
    },
})
export default class App extends Vue {
    title = "";

    mounted(): void {
        const touhouCreditElement = this.$refs.touhouCredit as HTMLElement;
        switch (getSiteType()) {
            case SiteType.REISEN:
                this.title = "Reisen";
                touhouCreditElement.innerHTML = `<a target="_blank" href="https://en.touhouwiki.net/wiki/Reisen_Udongein_Inaba">Reisen</a>`;
                break;
            case SiteType.TEWI:
                this.title =
                    "Tewi (actually just a carrot because I don't have a model for her :c)";
                touhouCreditElement.innerHTML = `<a target="_blank" href="https://en.touhouwiki.net/wiki/Tewi_Inaba">Tewi</a>`;
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
    font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
    user-select: none;
    pointer-events: none;
    color: rgba(100, 0, 200, 0.5);
}

#footer {
    position: absolute;
    bottom: 0;
    background: rgba(255, 255, 255, 0.8);
    padding: 3px;
    border-top-right-radius: 5px;
    min-height: 2em;
    font-family: sans-serif;
    min-width: 20em;
    user-select: none;
    pointer-events: none;
}

.credits-title {
    text-align: center;
    font-weight: bold;
}

#footer a {
    pointer-events: initial;
}

body {
    overflow: hidden;
    margin: 0;
}
</style>
