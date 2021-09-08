<template>
    <div
        ref="menuWrapper"
        class="menu-wrapper"
        tabindex="1"
        @keyup="onKeyUp($event)"
    >
        <div class="menu">
            <h1 class="title">{{ title }}</h1>
            <Separator />
            <div
                class="menu-button"
                @click="onResumeOrPlayClicked($event)"
                tabindex="2"
                :class="{ disabled: !canResume }"
            >
                {{ playing ? 'Resume' : 'Play' }}
            </div>
            <Separator />
            <Footer />
        </div>
    </div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import Footer from '@/components/menu/Footer.vue';
import Separator from '@/components/menu/Separator.vue';
import { getTitle } from '@/temp-util';

@Options({
    components: {
        Footer,
        Separator,
    },
    props: {
        playing: Boolean,
    },
})
export default class Menu extends Vue {
    title = getTitle();
    playing!: boolean;
    canResume = false;

    mounted(): void {
        (this.$refs.menuWrapper as HTMLElement).focus();
        this.canResume = !this.playing;
        if (!this.canResume) {
            setTimeout(() => (this.canResume = true), 2000);
        }
    }

    onResumeOrPlayClicked(): void {
        if (this.canResume) {
            this.$emit('close');
        }
    }

    onKeyUp(event: KeyboardEvent): void {
        if (this.canResume) {
            if (this.playing && event.code === 'Escape') {
                this.$emit('close');
            }
        }
    }
}
</script>

<style scoped lang="scss">
.menu-wrapper {
    z-index: 1000;
    position: absolute;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    outline: none;
    .menu {
        text-align: center;
        width: 80%;
        max-width: 40em;
        min-height: 2em;
        border-radius: 10px;
        background: rgba(0, 0, 0, 0.8);
        padding: 5px 20px;
        display: flex;
        flex-direction: column;
        .title {
            text-align: center;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            user-select: none;
            pointer-events: none;
            color: #eaf;
        }
        .menu-button {
            color: #aaa;
            font-family: sans-serif;
            font-size: 2em;
            cursor: pointer;
            text-align: center;
            user-select: none;
            padding: 0.5em 0;
            transition: color 0.5s;
            &.disabled {
                color: orangered;
                cursor: default;
            }
            &:not(.disabled):hover {
                color: #fff;
                background: rgba(255, 255, 255, 0.2);
            }
        }
    }
}
</style>

<style lang="scss">
.menu {
    a {
        color: cornflowerblue;
        &:visited {
            color: violet;
        }
    }
}
</style>
