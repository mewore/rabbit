<template>
    <div
        class="game-container"
        ref="container"
        @keydown="onKeyDown($event)"
        @keyup="onKeyUp($event)"
        @blur="onGameBlurred()"
    >
        <div class="analysis-recording-indicator" v-if="isAnalyzing">
            <q-spinner-grid color="green" size="2em"></q-spinner-grid>
            <span class="analysis-indicator-text">
                Analyzing... (press ` (backtick) again to stop)
            </span>
        </div>
        <div
            class="canvas-wrapper"
            ref="canvasWrapper"
            @keyup="onCanvasKeyUp($event)"
            @mousemove="onMouseMove($event)"
            @click="onClick()"
            @mousewheel="onMouseWheel($event)"
            tabindex="0"
            :style="{
                filter: `blur(${inactivity}vw) `,
                opacity: 1.0 - inactivity * 0.2,
            }"
        >
            <span
                v-for="label in labels"
                :key="label.id"
                class="floating-label"
                :style="{
                    top: label.top + 'px',
                    left: label.left + 'px',
                    opacity: label.opacity * (1.0 - inactivity) + '%',
                }"
            >
                {{ label.text }}
            </span>
        </div>
        <PerformanceDisplay
            :style="{ display: showingPerformance ? 'block' : 'none' }"
            ref="performanceDisplay"
        />
        <q-dialog
            ref="menuDialog"
            v-model="menuIsVisible"
            persistent
            @keyup="onDialogKeyUp($event)"
            transition-hide="fade"
        >
            <MainMenu
                ref="menu"
                :loadingAmmo="loadingAmmo"
                :ammoFailedToLoad="ammoFailedToLoad"
                :playing="playing"
                v-on:close="onMenuClosed()"
                v-on:settingsChange="onSettingsChanged($event)"
                v-on:menuChange="onMenuChanged()"
                :lastAnalyzedFrames="lastAnalyzedFrames"
            />
        </q-dialog>
    </div>
</template>

<script lang="ts">
import * as Ammo from 'ammo.js';
import { createToast } from 'mosha-vue-toastify';
import { QDialog, QSpinnerGrid } from 'quasar';
import { Options, Vue } from 'vue-class-component';

import MainMenu from '@/components/menu/MainMenu.vue';
import PerformanceDisplay from '@/components/PerformanceDisplay.vue';
import { FrameAnalysis } from '@/game/debug/frame-analysis';
import { FrameInfo } from '@/game/debug/frame-info';
import { getSettings, Settings } from '@/settings';
import { addCredit } from '@/temp-util';

import { GameScene } from '../game/game-scene';

interface LabelInfo {
    id: number;
    top: number;
    left: number;
    text: string;
    opacity: number;
}

const ACTIVE_FPS = 60;
const INACTIVE_FPS = 1;
const INACTIVITY_INCREASE_SPEED = 0.05;
const INACTIVITY_DECREASE_SPEED = 0.5;
const RESIZE_INACTIVITY_MULTIPLIER = 0.8;
const INACTIVITY_THRESHOLD = 0.98;
const INACTIVITY_LOW_THRESHOLD = 0.05;

const DEFAULT_FRAME_DELTA = 1 / ACTIVE_FPS;
const INACTIVE_FRAME_DELTA = 1 / INACTIVE_FPS;

@Options({
    components: {
        MainMenu,
        PerformanceDisplay,
        QDialog,
        QSpinnerGrid,
    },
    emits: ['darkUiSetting'],
})
export default class ReisenGame extends Vue {
    private webSocket?: WebSocket;
    private scene?: GameScene;
    private requestedAnimationFrame?: number;
    labels: LabelInfo[] = [];

    inactivity = 1;

    playing = false;
    menuIsVisible = true;

    showingPerformance = false;
    targetFrameDelta = INACTIVE_FRAME_DELTA;
    nextFrameMinTime = 0;

    private readonly frameAnalysis = FrameAnalysis.GLOBAL;
    isAnalyzing = false;
    lastAnalyzedFrames: FrameInfo[] = [];

    loadingAmmo = true;
    ammoFailedToLoad = false;

    private readonly eventsToRemove: [
        Node | Window,
        string,
        (event: unknown) => void
    ][] = [];

    async mounted(): Promise<void> {
        const settings = getSettings();
        if (!settings.darkUi) {
            this.$emit('darkUiSetting', settings.darkUi);
        }
        this.frameAnalysis.imageQuality = settings.frameAnalysisQuality;
        this.showingPerformance = settings.showPerformance;

        const canvasWrapper = this.getCanvasWrapper();

        ReisenGame.addAmmoJsCredits();
        // Ammo.js sometimes fails to load with a "TypeError: Cannot set properties of undefined (setting 'Ammo')"
        try {
            await (
                Ammo as unknown as (
                    target?: typeof Ammo
                ) => Promise<typeof Ammo>
            ).bind(Ammo)(Ammo);
        } catch (error) {
            let description =
                "There's nothing I could do to prevent this. Ammo.js randomly decided not to work. 😔";
            if ((error as Error).message) {
                description +=
                    '<br><br>The error is: ' + (error as Error).message;
            }
            createToast(
                { title: 'Failed to load Ammo.js!', description },
                { type: 'danger', timeout: -1, showCloseButton: false }
            );
            throw error;
        } finally {
            this.loadingAmmo = false;
        }

        this.webSocket = new WebSocket(
            `ws://${window.location.host}/multiplayer`
        );
        this.webSocket.binaryType = 'arraybuffer';
        this.scene = new GameScene(
            canvasWrapper,
            this.webSocket,
            settings,
            this.frameAnalysis
        );
        (this.$refs.performanceDisplay as PerformanceDisplay).start(
            this.scene.time
        );
        this.requestAnimationFrame();
        this.scene.input.active = !this.menuIsVisible;
        if (!this.menuIsVisible) {
            canvasWrapper.focus();
        }
        document.addEventListener(
            'blur',
            this.addEvent(document, 'blur', () => this.scene?.input.clear())
        );
        window.addEventListener(
            'resize',
            this.addEvent(window, 'resize', this.onResize.bind(this))
        );
        window.addEventListener(
            'blur',
            this.addEvent(window, 'blur', () => this.scene?.input.clear())
        );
        window.addEventListener(
            'focus',
            this.addEvent(window, 'focus', () => {
                if (!this.menuIsVisible) {
                    this.lockMouse();
                }
            })
        );
        document.addEventListener(
            'pointerlockchange',
            this.addEvent(document, 'pointerlockchange', () => {
                if (!this.scene) {
                    return;
                }
                this.menuIsVisible = !document.pointerLockElement;
                this.scene.input.active = !this.menuIsVisible;
                if (this.menuIsVisible) {
                    this.scene.input.clear();
                } else {
                    this.requestAnimationFrame();
                    canvasWrapper.focus();
                    setTimeout(() => {
                        canvasWrapper.focus();
                    });
                    if (!this.playing) {
                        this.playing = true;
                        this.scene.start();
                    }
                }
            })
        );
    }

    private static addAmmoJsCredits(): void {
        addCredit({
            thing: {
                text: 'Bullet physics engine',
                url: 'https://pybullet.org/wordpress/',
            },
            author: {
                text: 'erwincoumans',
                url: 'https://github.com/erwincoumans',
            },
        });

        addCredit({
            thing: {
                text: 'Ammo.js (JS port of Bullet)',
                url: 'https://github.com/kripken/ammo.js/',
            },
            author: { text: 'kripken', url: 'https://github.com/kripken' },
        });

        addCredit({
            thing: {
                text: 'ammojs-typed (TS type definitions of Ammo.js)',
                url: 'https://github.com/giniedp/ammojs-typed',
            },
            author: { text: 'Ginie', url: 'https://github.com/giniedp' },
        });
    }

    private addEvent<T extends Event>(
        element: Node | Window,
        eventType: string,
        callback: (event: T) => void
    ): (event: T) => void {
        this.eventsToRemove.push([
            element,
            eventType,
            callback as unknown as (event: unknown) => void,
        ]);
        return callback;
    }

    onMenuClosed(): void {
        this.lockMouse();
    }

    onSettingsChanged(newSettings: Settings): void {
        this.showingPerformance = newSettings.showPerformance;
        this.scene?.applySettings(newSettings);
        this.frameAnalysis.imageQuality = newSettings.frameAnalysisQuality;
        this.$emit('darkUiSetting', newSettings.darkUi);
    }

    onMenuChanged(): void {
        (this.$refs.menuDialog as { shake: () => void }).shake();
    }

    private getCanvasWrapper(): HTMLElement {
        return this.$refs.canvasWrapper as HTMLElement;
    }

    private lockMouse(): void {
        if (this.scene && !document.pointerLockElement) {
            this.getCanvasWrapper().requestPointerLock();
        }
    }

    private animate(): void {
        this.requestedAnimationFrame = undefined;
        if (!this.scene) {
            return;
        }
        this.requestAnimationFrame();
        const performanceDisplay = this.$refs
            .performanceDisplay as PerformanceDisplay;
        const time = this.scene.time;
        if (time < this.nextFrameMinTime) {
            performanceDisplay.registerFrame(
                time,
                this.targetFrameDelta,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                true
            );
            return;
        }
        this.scene.animate();
        const rendererInfo = this.scene.renderer.info;
        performanceDisplay.registerFrame(
            time,
            this.targetFrameDelta,
            rendererInfo.render.calls,
            this.scene.forest.totalPlants,
            this.scene.forest.renderedDetailedPlants,
            this.scene.forest.renderedDummyPlants,
            this.scene.physicsBodyCount,
            this.scene.activeForestWallBodyCount,
            this.scene.totalForestWallBodyCount
        );
        if (
            this.menuIsVisible &&
            !(this.$refs.menu as MainMenu).isEditingGraphics()
        ) {
            if (this.inactivity < 1.0) {
                this.inactivity =
                    this.inactivity > INACTIVITY_THRESHOLD
                        ? 1.0
                        : INACTIVITY_INCREASE_SPEED +
                          (1.0 - INACTIVITY_INCREASE_SPEED) * this.inactivity;
            }
            const targetFps =
                this.inactivity * INACTIVE_FPS +
                (1.0 - this.inactivity) * ACTIVE_FPS;
            this.targetFrameDelta = 1 / targetFps;
            this.nextFrameMinTime = time + this.targetFrameDelta;
        } else {
            this.inactivity *= 1.0 - INACTIVITY_DECREASE_SPEED;
            if (this.inactivity < INACTIVITY_LOW_THRESHOLD) {
                this.inactivity = 0.0;
            }
            this.targetFrameDelta = DEFAULT_FRAME_DELTA;
        }
        const width = this.scene.getWidth();
        const height = this.scene.getHeight();
        let labelIndex = 0;
        this.scene.forEveryPlayerLabel((position, username) => {
            const closenessCoefficient = 1.0 - position.z;
            if (closenessCoefficient < 0.0) {
                return;
            }
            const left = Math.round(position.x * width);
            const top = Math.round(position.y * height);
            const opacity = Math.round(closenessCoefficient * 100);
            if (labelIndex >= this.labels.length) {
                this.labels.push({} as LabelInfo);
            }
            const label = this.labels[labelIndex];
            label.id = labelIndex;
            label.left = left;
            label.top = top;
            label.text = username;
            label.opacity = opacity;
            labelIndex++;
        });
        this.labels.splice(labelIndex);
    }

    private requestAnimationFrame(): void {
        if (this.requestedAnimationFrame) {
            cancelAnimationFrame(this.requestedAnimationFrame);
        }
        this.requestedAnimationFrame = requestAnimationFrame(
            this.animate.bind(this)
        );
    }

    beforeUnmount(): void {
        this.scene = undefined;
        if (typeof this.requestedAnimationFrame === 'number') {
            cancelAnimationFrame(this.requestedAnimationFrame);
        }
        this.webSocket?.close();
        for (const toRemove of this.eventsToRemove) {
            toRemove[0].removeEventListener(toRemove[1], toRemove[2]);
        }
    }

    onGameBlurred(): void {
        this.scene?.input.clear();
    }

    onKeyDown(event: KeyboardEvent): void {
        this.setKeyValue(event.code, true);
    }

    onKeyUp(event: KeyboardEvent): void {
        this.setKeyValue(event.code, false);
    }

    onDialogKeyUp(event: KeyboardEvent): void {
        if (this.menuIsVisible) {
            if (event.code === 'Backquote') {
                this.toggleAnalysis();
                return;
            }
            (this.$refs.menu as MainMenu).onKeyUp(event);
        }
    }

    onCanvasKeyUp(event: KeyboardEvent): void {
        if (event.code === 'Escape') {
            this.menuIsVisible = true;
            if (this.scene) {
                this.scene.input.active = false;
            }
        } else if (event.code === 'Backquote') {
            this.toggleAnalysis();
        }
    }

    onMouseMove(event: MouseEvent): void {
        if (this.scene && !this.menuIsVisible) {
            this.scene.input.processMouseMovement(
                event.movementX,
                event.movementY
            );
        }
    }

    onClick(): void {
        if (!this.menuIsVisible) {
            this.lockMouse();
        }
    }

    onMouseWheel(event: WheelEvent): void {
        this.scene?.input.processMouseWheel(
            (event as unknown as { wheelDelta: number }).wheelDelta
        );
    }

    onResize(): void {
        if (this.scene) {
            this.inactivity *= RESIZE_INACTIVITY_MULTIPLIER;
            this.scene.refreshSize();
        }
    }

    setKeyValue(keyCode: string, isDown: boolean): void {
        if (!this.scene) {
            return;
        }
        this.scene.input.processKey(keyCode, isDown);
    }

    private toggleAnalysis(): void {
        if (this.frameAnalysis.analyzing) {
            this.lastAnalyzedFrames = this.frameAnalysis.complete();
            createToast(
                `Analysis of ${this.lastAnalyzedFrames.length} frame${
                    this.lastAnalyzedFrames.length > 0 ? 's' : ''
                } complete.` + ' Open the menu to see it.',
                {
                    type: 'success',
                    showIcon: true,
                    position: 'top-center',
                }
            );
        } else if (this.frameAnalysis.imageQuality > 0) {
            this.frameAnalysis.start();
        }
        this.isAnalyzing = this.frameAnalysis.analyzing;
    }
}
</script>

<style scoped lang="scss">
.canvas-wrapper {
    position: absolute;
    height: 100vh;
    width: 100vw;
}
.floating-label {
    position: absolute;
    font-weight: bold;
    user-select: none;
    pointer-events: none;
    font-family: sans-serif;
    min-width: 6em;
    margin-left: -3em;
    height: 1.2em;
    margin-top: -0.6em;
    line-height: 1.2em;
    background: black;
    padding: 5px;
    border-radius: 10px;
    color: white;
    text-align: center;
}
.game-container {
    width: 100%;
    height: 100%;
    position: absolute;

    .analysis-recording-indicator {
        position: absolute;
        top: 10px;
        left: 10px;
        z-index: 7000;
        font-weight: bold;

        .analysis-indicator-text {
            margin-left: 1em;
        }
    }
}
</style>
