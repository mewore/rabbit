<template>
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
import { Options, Vue } from 'vue-class-component';
import { SiteType, getSiteType } from '@/temp-util';

@Options({})
export default class Footer extends Vue {
    mounted(): void {
        const touhouCreditElement = this.$refs.touhouCredit as HTMLElement;
        switch (getSiteType()) {
            case SiteType.REISEN:
                touhouCreditElement.innerHTML =
                    '<a target="_blank" href="https://en.touhouwiki.net/wiki/Reisen_Udongein_Inaba">Reisen</a>';
                break;
            case SiteType.TEWI:
                touhouCreditElement.innerHTML =
                    '<a target="_blank" href="https://en.touhouwiki.net/wiki/Tewi_Inaba">Tewi</a>';
                break;
        }
    }
}
</script>

<style scoped lang="scss">
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
</style>
