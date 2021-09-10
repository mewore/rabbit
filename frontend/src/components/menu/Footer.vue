<template>
    <div id="footer" ref="footer">
        <div class="footer-title">Credits</div>
        <div class="credit" v-for="credit in credits" :key="credit">
            <div>
                <a
                    v-if="credit.thing.url"
                    target="_blank"
                    :href="credit.thing.url"
                >
                    {{ credit.thing.text }}
                </a>
                <span v-else>{{ credit.thing.text }}</span>
                by
                <a
                    v-if="credit.author.url"
                    target="_blank"
                    :href="credit.author.url"
                >
                    {{ credit.author.text }}
                </a>
                <span v-else>{{ credit.author.text }}</span>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import { getCredits } from '@/temp-util';

@Options({})
export default class Footer extends Vue {
    credits = getCredits();
}
</script>

<style scoped lang="scss">
#footer {
    color: #ddd;
    padding: 3px;
    min-height: 2em;
    font-family: sans-serif;
    min-width: 20em;
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
