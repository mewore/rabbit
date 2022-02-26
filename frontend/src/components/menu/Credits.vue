<template>
    <div id="credits">
        <div class="credit" v-for="credit in credits" :key="credit.thing.text">
            <a
                v-if="credit.thing.url"
                target="_blank"
                :href="credit.thing.url"
                class="credit-thing"
            >
                {{ credit.thing.text }}
            </a>
            <span v-else class="credit-thing">{{ credit.thing.text }}</span>
            <span class="credit-by"> by </span>
            <a
                v-if="credit.author.url"
                target="_blank"
                :href="credit.author.url"
                class="credit-author"
            >
                {{ credit.author.text }}
            </a>
            <span v-else class="credit-author">{{ credit.author.text }}</span>
        </div>
    </div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import { getCredits } from '@/temp-util';

@Options({})
export default class Credits extends Vue {
    credits = getCredits();
}
</script>

<style scoped lang="scss">
#credits {
    color: #ddd;
    padding: 3px;
    min-height: 2em;
    font-family: sans-serif;
    min-width: 20em;
    .credit {
        display: flex;
        .credit-thing {
            flex: 1;
            text-align: right;
        }
        .credit-by {
            white-space: pre;
        }
        .credit-author {
            flex: 1;
            text-align: left;
        }
    }
}
</style>

<style lang="scss">
#credits a {
    pointer-events: initial;
}
</style>
