<template>
    <div
        class="separator"
        :style="{
            margin: `${size}em 0`,
        }"
    ></div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';

@Options({})
export default class Separator extends Vue {
    size = 1;
}
</script>

<style scoped lang="scss">
.separator {
    height: 0.05em;
    background: gray;
    width: 100%;
}
</style>
