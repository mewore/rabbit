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
                        @change="
                            (value) => {
                                settings.quality = value;
                                onUpdated();
                            }
                        "
                        :min="0.1"
                        :max="1.0"
                        :step="0"
                        color="purple-4"
                    />
                    <q-tooltip
                        >A lower clarity results in a blurry/jagged
                        appearance.</q-tooltip
                    >
                </div>
                <q-toggle
                    v-model="settings.shadows"
                    label="Render shadows"
                    @update:model-value="onUpdated()"
                />
                <q-toggle
                    v-model="settings.darkUi"
                    label="Dark UI"
                    @update:model-value="onUpdated()"
                />
            </q-tab-panel>

            <q-tab-panel name="debug">
                <q-toggle
                    v-model="settings.showPerformance"
                    label="Show performance info"
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
import { Options, Vue } from 'vue-class-component';
import { SaveLocation, getSettings, setSettings } from '@/temp-util';

@Options({
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

    onUpdated(): void {
        this.$emit('settingsChange', this.settings);
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
        .text-subtitle1 {
            padding: 0 1em;
            cursor: default;
        }
    }
    height: 10em;
}
</style>
