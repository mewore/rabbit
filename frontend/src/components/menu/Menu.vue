<template>
    <h1 class="title">{{ title }}</h1>
    <q-card class="menu" tabindex="1" ref="menu">
        <q-bar>
            <q-space />
            <div class="menu-name">{{ menuName }}</div>
            <q-space />
        </q-bar>

        <template v-if="currentMenu === 'MAIN_MENU'">
            <q-btn-group class="button-group" spread>
                <q-btn
                    :disable="playing && !canResume"
                    color="purple"
                    @click="onResumeOrPlayClicked($event)"
                    tabindex="2"
                    size="lg"
                    :label="playing ? 'Resume' : 'Play'"
                >
                    <div class="button-sublabel" v-if="playing">
                        {{
                            canResume
                                ? resumeButtonTip
                                : 'Waiting for the Pointer Lock API to chill out...'
                        }}
                    </div></q-btn
                >

                <q-btn
                    @click="onTogglePerformanceDisplayClicked($event)"
                    tabindex="3"
                    size="lg"
                    :label="
                        (showingPerformance ? 'Hide' : 'Show') +
                        ' performance info'
                    "
                />
                <q-btn
                    @click="goTo('CREDITS')"
                    tabindex="4"
                    size="lg"
                    label="Credits"
                />
            </q-btn-group>
        </template>
        <template v-if="currentMenu === 'CREDITS'">
            <q-btn-group class="button-group" spread>
                <q-btn
                    :disable="playing && !canResume"
                    color="purple"
                    @click="onResumeOrPlayClicked($event)"
                    tabindex="2"
                    size="lg"
                    :label="playing ? 'Resume' : 'Play'"
                >
                    <div class="button-sublabel" v-if="playing">
                        {{
                            canResume
                                ? resumeButtonTip
                                : 'Waiting for the Pointer Lock API to chill out...'
                        }}
                    </div></q-btn
                >
                <q-btn
                    @click="goTo('MAIN_MENU')"
                    tabindex="4"
                    size="lg"
                    label="Back"
                />
            </q-btn-group>
            <q-card-section>
                <Credits />
            </q-card-section>
        </template>
    </q-card>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import Credits from '@/components/menu/Credits.vue';
import Separator from '@/components/menu/Separator.vue';
import { getTitle } from '@/temp-util';

@Options({
    components: {
        Credits,
        Separator,
    },
    props: {
        playing: Boolean,
        showingPerformance: Boolean,
    },
    emits: ['close', 'performanceDisplayToggled'],
})
export default class Menu extends Vue {
    title = getTitle();
    currentMenu = 'MAIN_MENU';
    menuName = 'Main Menu';

    playing!: boolean;
    showingPerformance!: boolean;
    canResume = false;

    resumeButtonTip = 'or press [Escape]';
    private readonly pointerLockErrorHandler = () => {
        this.resumeButtonTip =
            "The Pointer Lock API doesn't want you to use [Escape] this time. Try clicking instead.";
    };

    mounted(): void {
        (this.$refs.menu as { $el: HTMLElement }).$el.focus();
        this.canResume = !this.playing;
        if (!this.canResume) {
            setTimeout(() => (this.canResume = true), 1500);
        }
        document.addEventListener(
            'pointerlockerror',
            this.pointerLockErrorHandler
        );
    }

    beforeUnmount(): void {
        document.removeEventListener(
            'pointerlockerror',
            this.pointerLockErrorHandler
        );
    }

    onResumeOrPlayClicked(): void {
        if (this.canResume) {
            this.$emit('close');
        }
    }

    onTogglePerformanceDisplayClicked(): void {
        this.$emit('performanceDisplayToggled');
    }

    onKeyUp(event: KeyboardEvent): void {
        if (this.canResume && event.code === 'Escape' && this.playing) {
            this.$emit('close');
        }
    }

    goTo(menu: string): void {
        this.currentMenu = menu;
        switch (menu) {
            case 'MAIN_MENU':
                this.menuName = 'Main Menu';
                break;
            case 'SETTINGS':
                this.menuName = 'Settings';
                break;
            case 'CREDITS':
                this.menuName = 'Credits';
                break;
        }
    }
}
</script>

<style scoped lang="scss">
.title {
    text-align: center;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    user-select: none;
    pointer-events: none;
    color: #eaf;
    opacity: 0.8;
    font-weight: bold;
    position: absolute;
    top: 0;
}
.menu {
    text-align: center;
    width: 80%;
    max-width: 45em;
    outline: none;
    .menu-name {
        user-select: none;
        pointer-events: none;
    }
    .button-group {
        user-select: none;
        flex-direction: column;
        .button-sublabel {
            padding-top: 0.4em;
            font-size: 60%;
            width: 100%;
            line-height: 100%;
            opacity: 0.8;
        }
        &:not(:last-child) {
            border-bottom: rgba(125, 125, 125, 0.4) 1px dashed;
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
