<template>
    <div id="footer" ref="footer">
        <div class="footer-title">Credits</div>
        <div>
            <a target="_blank" href="https://threejs.org/">Three.js</a> by
            <a target="_blank" href="https://github.com/mrdoob">Mr.doob</a>
        </div>
        <div>
            <span ref="touhouCredit">Touhou</span> by
            <a target="_blank" href="https://en.touhouwiki.net/wiki/ZUN">ZUN</a>
        </div>
        <div><a target="_blank" href="https://threejs.org/">Three.js</a></div>
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
                this.addFumoCredit();
                break;
            case SiteType.TEWI:
                touhouCreditElement.innerHTML =
                    '<a target="_blank" href="https://en.touhouwiki.net/wiki/Tewi_Inaba">Tewi</a>';
                this.addTewiCredit();
                break;
        }
    }
    private addFumoCredit(): void {
        const fumo = '<a target="_blank" href="https://fumo.website/">Fumo</a>';
        const royalcat =
            '<a target="_blank" href="https://royalcat.xyz/">ROYALCAT</a>';
        const angeltype =
            '<a target="_blank" href="http://blog.angeltype.under.jp/">ANGELTYPE</a>';
        this.addCredit(
            `${fumo} design by ${royalcat} (also known as ${angeltype})`
        );
    }

    private addTewiCredit(): void {
        const carrotUrl =
            'https://sketchfab.com/3d-models/low-poly-carrot-31df366e091a4e64b9b0cfc1afc0145d';
        const authorUrl = 'https://sketchfab.com/thepianomonster';
        this.addCredit(
            `<a href="${carrotUrl}" target="_blank">Carrot model</a> ` +
                `by <a href="${authorUrl}" target="_blank">thepianomonster</a>`
        );
    }

    private addCredit(creditHtml: string): void {
        const newElement = document.createElement('div');
        newElement.innerHTML = creditHtml;
        (this.$refs.footer as HTMLElement).appendChild(newElement);
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
    .footer-title {
        text-align: center;
        font-weight: bold;
    }
}
</style>

<style lang="scss">
#footer a {
    pointer-events: initial;
}
</style>
