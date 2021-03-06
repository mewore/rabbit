<template>
    <q-card>
        <q-tabs
            v-model="tab"
            active-color="purple-4"
            indicator-color="purple-4"
            align="justify"
            narrow-indicator
        >
            <q-tab name="general" label="General" />
            <q-tab name="graphics" label="Graphics" />
            <q-tab name="physics" label="Physics" />
            <q-tab name="debug" label="Debug" />
        </q-tabs>

        <q-separator />

        <q-tab-panels v-model="tab" animated class="settings-content">
            <q-tab-panel name="general">
                <q-select
                    filled
                    v-model="saveTo"
                    :options="saveToOptions"
                    label="Remember settings"
                    color="purple-4"
                    options-selected-class="text-purple-4"
                >
                    <template v-slot:option="scope">
                        <q-item v-bind="scope.itemProps">
                            <q-item-section>
                                <q-item-label>{{
                                    scope.opt.label
                                }}</q-item-label>
                                <q-item-label caption>{{
                                    scope.opt.description
                                }}</q-item-label>
                            </q-item-section>
                        </q-item>
                    </template>
                </q-select>
            </q-tab-panel>

            <q-tab-panel name="graphics">
                <div class="input-with-label">
                    <div class="text-subtitle1">Clarity</div>
                    <q-slider
                        :model-value="settings.quality"
                        @change="onQualityChanged"
                        :min="0.1"
                        :max="1.0"
                        :step="0.05"
                        label
                        markers
                        :marker-labels="makeMarkers(0.1, 1.0)"
                        :label-value="makePercentLabel(settings.quality)"
                        switch-label-side
                        color="purple-4"
                    />
                    <div class="text-purple-4 right-icon">
                        <q-icon name="info" />
                        <q-tooltip>
                            A lower clarity results in a blurry/jagged
                            appearance.
                        </q-tooltip>
                    </div>
                </div>
                <div class="input-with-label">
                    <div class="text-subtitle1">Visible plants</div>
                    <q-slider
                        v-model="settings.plantVisibility"
                        :min="0"
                        :max="1"
                        :step="0.05"
                        label
                        markers
                        :marker-labels="{ 0: 'None', 1: 'All' }"
                        :label-value="
                            makePercentLabel(settings.plantVisibility)
                        "
                        @update:model-value="onUpdated()"
                        color="purple-4"
                    />
                </div>
                <q-toggle
                    v-model="settings.shadows"
                    label="Render shadows"
                    @update:model-value="onUpdated()"
                />
                <q-toggle
                    v-model="settings.plantsReceiveShadows"
                    label="Plants receive shadows"
                    :disable="!settings.shadows"
                    @update:model-value="onUpdated()"
                />
                <q-toggle
                    v-model="settings.darkUi"
                    label="Dark UI"
                    @update:model-value="onUpdated()"
                />
            </q-tab-panel>

            <q-tab-panel name="physics">
                <div class="input-with-label">
                    <div class="text-subtitle1">Forest wall active radius</div>
                    <q-slider
                        v-model="settings.forestWallActiveRadius"
                        :min="50"
                        :max="500"
                        :step="50"
                        label
                        markers
                        :label-value="settings.forestWallActiveRadius"
                        @update:model-value="onUpdated()"
                        color="purple-4"
                    />
                    <div class="text-purple-4 right-icon">
                        <q-icon name="info" />
                        <q-tooltip>
                            The physics bodies of forest walls far away from
                            your character are deactivated. The worst thing that
                            can happen with this radius being low is that other
                            players may seem to be going through walls
                            temporarily from your perspective.
                        </q-tooltip>
                    </div>
                </div>
            </q-tab-panel>

            <q-tab-panel name="debug">
                <div class="input-with-label">
                    <div class="text-subtitle1">Artificial latency</div>
                    <q-slider
                        v-model="settings.artificialLatency"
                        :min="0"
                        :max="2000"
                        :step="50"
                        label
                        :label-value="settings.artificialLatency + ' ms'"
                        :marker-labels="{
                            0: 'None',
                            300: '300 ms',
                            1000: '1 sec',
                            2000: '2s',
                        }"
                        switch-label-side
                        @update:model-value="onUpdated()"
                        color="purple-4"
                    />
                    <div class="text-purple-4 right-icon">
                        <q-icon name="info" />
                        <q-tooltip>
                            Delay the data between yourself and the server in
                            order to test extreme scenarios. This affects only
                            the data sent in the WebSocket; the normal HTTP
                            requests are not affected.
                        </q-tooltip>
                    </div>
                </div>
                <div class="input-with-label">
                    <div class="text-subtitle1">
                        Frame analysis image quality
                    </div>
                    <q-slider
                        v-model="settings.frameAnalysisQuality"
                        :min="0.0"
                        :max="1.0"
                        :step="0.1"
                        label
                        :label-value="`${Math.round(
                            settings.frameAnalysisQuality * 100
                        )}%`"
                        markers
                        switch-label-side
                        @update:model-value="onUpdated()"
                        color="purple-4"
                    />
                    <div class="text-purple-4 right-icon">
                        <q-icon name="info" />
                        <q-tooltip>
                            This determines the quality of the frame snapshots
                            (a higher quality may cause more lag).
                            <strong>Press ` (backtick)</strong> to start
                            recording frames and ` again to stop. Then, you can
                            view the frame-by-frame analysis by going to the
                            "Frame Analysis" menu.
                            <strong>
                                With the quality set to 0%, you cannot do any
                                frame analysis.
                            </strong>
                        </q-tooltip>
                    </div>
                </div>
                <q-toggle
                    v-model="settings.showPerformance"
                    label="Show performance info"
                    @update:model-value="onUpdated()"
                />
                <q-toggle
                    v-model="settings.debugPhysics"
                    label="Debug physics"
                    @update:model-value="onUpdated()"
                />
            </q-tab-panel>
        </q-tab-panels>
    </q-card>
    <q-card-actions align="right">
        <q-btn color="purple" glossy label="Save" @click="onSaveClicked()" />
        <q-btn
            outline
            color="brown"
            label="Cancel"
            @click="onCancelClicked()"
        />
    </q-card-actions>
</template>

<script lang="ts">
import {
    QBtn,
    QCard,
    QCardActions,
    QIcon,
    QItem,
    QItemLabel,
    QItemSection,
    QSelect,
    QSeparator,
    QSlider,
    QTab,
    QTabPanel,
    QTabPanels,
    QTabs,
    QToggle,
    QTooltip,
} from 'quasar';
import { Options, Vue } from 'vue-class-component';

import { getSettings, SaveLocation, setSettings } from '@/settings';

@Options({
    components: {
        QBtn,
        QCard,
        QCardActions,
        QIcon,
        QItem,
        QItemLabel,
        QItemSection,
        QSelect,
        QSeparator,
        QSlider,
        QTab,
        QTabPanel,
        QTabPanels,
        QTabs,
        QToggle,
        QTooltip,
    },
    emits: ['close', 'settingsChange'],
})
export default class SettingsMenu extends Vue {
    tab = 'general';
    settings = getSettings();

    saveToOptions = [
        {
            label: 'Never',
            value: SaveLocation.NOWHERE,
            description: 'Do not remember the settings.',
        },
        {
            label: 'During this session',
            value: SaveLocation.SESSION,
            description:
                'Remember the settings when this tab is refreshed or duplicated.',
        },
        {
            label: 'Forever',
            value: SaveLocation.STORAGE,
            description:
                'Remember the settings even after the browser is closed.',
        },
    ];

    saveTo =
        this.saveToOptions.find(
            (option) => option.value === this.settings.saveTo
        ) || this.saveToOptions[0];

    makeMarkers(...values: number[]): { [value: number]: string } {
        const result: { [value: number]: string } = {};
        for (const value of values) {
            result[value] = this.makePercentLabel(value);
        }
        return result;
    }

    makePercentLabel(value: number): string {
        return Math.round(value * 100) + '%';
    }

    onUpdated(): void {
        this.$emit('settingsChange', this.settings);
    }

    onQualityChanged(newQuality: number): void {
        this.settings.quality = newQuality;
        this.onUpdated();
    }

    onSaveClicked(): void {
        this.settings.saveTo = this.saveTo.value;
        setSettings(this.settings);
        this.$emit('close');
    }

    onCancelClicked(): void {
        this.$emit('settingsChange', getSettings());
        this.$emit('close');
    }
}
</script>

<style scoped lang="scss">
.settings-content {
    text-align: left;
    .q-separator {
        margin: 1em 0;
    }
    .subtitle {
        margin-bottom: 1em;
    }
    .input-with-label {
        display: flex;
        flex-wrap: nowrap;
        margin-bottom: 0.5em;
        align-items: center;
        .text-subtitle1 {
            padding: 0 1em;
            cursor: default;
            min-width: min(50%, 10em);
        }
        .right-icon {
            padding-left: 0.5em;
            font-size: 2em;
        }
    }
    height: 15em;
}
.q-tab-panel {
    overflow-x: hidden;
}
</style>

<style lang="scss">
.settings-content {
    .q-slider__marker-labels-container {
        opacity: 0.5;
    }
}
</style>
