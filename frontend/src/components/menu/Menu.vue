<template>
    <h1 class="title">{{ title }}</h1>
    <q-card
        :class="{ menu: true, large: currentMenu === 'ANALYSIS' }"
        tabindex="1"
        ref="menu"
    >
        <q-bar>
            <q-space />
            <div class="menu-name">{{ menuName }}</div>
            <q-space />
        </q-bar>

        <q-btn-group
            class="button-group"
            spread
            v-if="menusWithStandardButtons.indexOf(currentMenu) >= 0"
        >
            <q-btn
                :disable="playing && !canResume"
                color="purple"
                @click="onResumeOrPlayClicked()"
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

            <template v-if="currentMenu === 'MAIN_MENU'">
                <q-btn
                    @click="goTo('SETTINGS')"
                    tabindex="4"
                    size="lg"
                    label="Settings"
                />
                <q-btn
                    @click="goTo('WORLD_EDITOR')"
                    tabindex="4"
                    size="lg"
                    label="World Editor"
                />
                <q-btn
                    @click="goTo('CREDITS')"
                    tabindex="5"
                    size="lg"
                    label="Credits"
                />
                <q-btn
                    v-if="lastAnalyzedFrames.length"
                    @click="goTo('ANALYSIS')"
                    tabindex="6"
                    size="lg"
                    label="Frame Analysis"
                />
            </template>
            <template v-else>
                <q-btn
                    @click="goTo('MAIN_MENU')"
                    tabindex="4"
                    size="lg"
                    label="Back"
                />
            </template>
        </q-btn-group>

        <template v-if="currentMenu === 'CREDITS'">
            <q-card-section>
                <Credits ref="creditsMenu" />
            </q-card-section>
        </template>
        <template v-if="currentMenu === 'SETTINGS'">
            <SettingsMenu
                ref="settingsMenu"
                v-on:close="goTo('MAIN_MENU')"
                v-on:settingsChange="onSettingsChanged"
            />
        </template>
        <template v-if="currentMenu === 'WORLD_EDITOR'">
            <EditorDownloadMenu
                ref="editorMenu"
                v-on:close="goTo('MAIN_MENU')"
            />
        </template>
        <template
            v-if="currentMenu === 'ANALYSIS' && lastAnalyzedFrames.length"
        >
            <q-card-section>
                <FrameAnalysisMenu :frames="lastAnalyzedFrames" />
            </q-card-section>
        </template>
    </q-card>
</template>

<script lang="ts">
import { QBar, QBtn, QBtnGroup, QCard, QCardSection, QSpace } from 'quasar';
import { Options, Vue } from 'vue-class-component';

import { FrameInfo } from '@/game/debug/frame-info';
import { Settings } from '@/settings';
import { getTitle } from '@/temp-util';

import Credits from './Credits.vue';
import EditorDownloadMenu from './EditorDownloadMenu.vue';
import FrameAnalysisMenu from './FrameAnalysisMenu.vue';
import SettingsMenu from './SettingsMenu.vue';

type MenuId =
    | 'MAIN_MENU'
    | 'SETTINGS'
    | 'WORLD_EDITOR'
    | 'CREDITS'
    | 'ANALYSIS';

@Options({
    components: {
        Credits,
        EditorDownloadMenu,
        FrameAnalysisMenu,
        SettingsMenu,
        QBar,
        QBtn,
        QBtnGroup,
        QCard,
        QCardSection,
        QSpace,
    },
    props: {
        playing: Boolean,
        showingPerformance: Boolean,
        lastAnalyzedFrames: Array,
    },
    emits: ['close', 'settingsChange', 'menuChange'],
})
export default class Menu extends Vue {
    title = getTitle();
    currentMenu: MenuId = 'MAIN_MENU';
    menuName = 'Main Menu';
    menusWithStandardButtons: MenuId[] = ['MAIN_MENU', 'CREDITS', 'ANALYSIS'];

    lastAnalyzedFrames!: FrameInfo[];

    playing!: boolean;
    canResume = false;

    resumeButtonTip = 'or press [Escape]';
    private readonly pointerLockErrorHandler = () => {
        this.resumeButtonTip =
            "The Pointer Lock API doesn't want you to use [Escape] this time. Try clicking instead.";
    };

    isEditingGraphics(): boolean {
        if (this.currentMenu !== 'SETTINGS') {
            return false;
        }
        const tab = (this.$refs.settingsMenu as SettingsMenu).tab;
        return tab === 'graphics' || tab === 'debug';
    }

    onSettingsChanged(newSettings: Settings): void {
        this.$emit('settingsChange', newSettings);
    }

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

    onKeyUp(event: KeyboardEvent): void {
        if (event.code === 'Escape') {
            switch (this.currentMenu) {
                case 'MAIN_MENU':
                    if (this.canResume && this.playing) {
                        this.$emit('close');
                    }
                    break;
                case 'SETTINGS':
                    (this.$refs.settingsMenu as SettingsMenu).onCancelClicked();
                    break;
                case 'WORLD_EDITOR':
                    (
                        this.$refs.editorMenu as EditorDownloadMenu
                    ).onBackClicked();
                    break;
                case 'CREDITS':
                case 'ANALYSIS':
                    this.goTo('MAIN_MENU');
                    break;
            }
        }
    }

    goTo(menu: MenuId): void {
        if (menu === this.currentMenu) {
            return;
        }
        this.$emit('menuChange');
        this.currentMenu = menu;
        this.menuName = Menu.getMenuName(menu);
        (this.$refs.menu as { $el: HTMLElement }).$el.focus();
    }

    private static getMenuName(menu: MenuId): string {
        switch (menu) {
            case 'MAIN_MENU':
                return 'Main Menu';
            case 'SETTINGS':
                return 'Settings';
            case 'WORLD_EDITOR':
                return 'World Editor';
            case 'CREDITS':
                return 'Credits';
            case 'ANALYSIS':
                return 'Frame Analysis';
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
    width: 90%;
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

    &.large {
        max-width: 100vw;
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
