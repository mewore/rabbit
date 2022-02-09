<template>
    <q-card-section class="settings-content">
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
                        <q-item-label>{{ scope.opt.label }}</q-item-label>
                        <q-item-label caption>{{
                            scope.opt.description
                        }}</q-item-label>
                    </q-item-section>
                </q-item>
            </template>
        </q-select>

        <q-toggle
            v-model="settings.showPerformance"
            label="Show performance info"
            @update:model-value="onUpdated()"
        />
    </q-card-section>
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
}
</style>
