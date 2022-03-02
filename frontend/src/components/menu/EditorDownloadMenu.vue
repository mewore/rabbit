<template>
    <q-card>
        <q-select
            filled
            v-model="currentOption"
            :options="options"
            label="Version"
            stack-label
            :dense="true"
            :options-dense="true"
            :disable="loading || noVersions"
            @update:model-value="updateSelectedVersion()"
        />

        <q-separator />

        <div class="q-pa-md">
            <div v-if="loading">
                <div class="col unavailable">
                    <q-spinner size="4em" /><br />
                    Loading...
                </div>
            </div>
            <div class="row" v-else-if="currentVersion">
                <a
                    class="col available"
                    v-if="currentVersion?.windowsZipUrl"
                    :href="currentVersion.windowsZipUrl"
                >
                    <q-icon name="download" size="4em" color="purple" /><br />
                    Windows x64 ZIP
                </a>
                <div class="col unavailable" v-else>
                    <q-icon name="error_outline" size="4em" /><br />
                    No Windows x64 ZIP
                </div>

                <a
                    class="col available"
                    v-if="currentVersion?.linuxTarballUrl"
                    :href="currentVersion.linuxTarballUrl"
                >
                    <q-icon name="download" size="4em" color="purple" /><br />
                    Linux x64 tarball
                </a>
                <div class="col unavailable" v-else>
                    <q-icon name="error_outline" size="4em" /><br />
                    No Linux x64 tarball
                </div>

                <a
                    class="col available"
                    v-if="currentVersion?.jarUrl"
                    :href="currentVersion.jarUrl"
                >
                    <q-icon name="download" size="4em" color="purple" /><br />
                    JAR
                </a>
                <div class="col unavailable" v-else>
                    <q-icon name="error_outline" size="4em" /><br />
                    No JAR
                </div>
            </div>
        </div>
    </q-card>
    <q-card-actions align="right">
        <q-btn outline color="brown" label="Back" @click="onBackClicked()" />
    </q-card-actions>
</template>

<script lang="ts">
import axios from 'axios';
import {
    QBtn,
    QCard,
    QCardActions,
    QIcon,
    QSelect,
    QSeparator,
    QSpinner,
} from 'quasar';
import { QSelectOption } from 'quasar/dist/types/api';
import { Options, Vue } from 'vue-class-component';

const LOADING_OPTION: QSelectOption<number> = {
    value: -1,
    label: 'Loading versions...',
};

const NO_VERSIONS_OPTION: QSelectOption<number> = {
    value: -1,
    label: 'No available world editor downloads',
};
const EMPTY_VERSION: EditorVersion = {
    id: NO_VERSIONS_OPTION.value,
    lastModified: -1,
};

interface EditorVersion {
    id: number;
    lastModified: number;
    windowsZipUrl?: string;
    linuxTarballUrl?: string;
    jarUrl?: string;
}

@Options({
    components: {
        QBtn,
        QCard,
        QCardActions,
        QIcon,
        QSelect,
        QSeparator,
        QSpinner,
    },
    emits: ['close'],
})
export default class EditorDownloadMenu extends Vue {
    loading = true;
    noVersions = false;

    options: QSelectOption<number>[] = [LOADING_OPTION];
    currentOption = LOADING_OPTION;
    private versionsById: Map<number, EditorVersion> = new Map();
    currentVersion?: EditorVersion;

    mounted(): void {
        Promise.all([this.fetchVersions()]).then(() => (this.loading = false));
    }

    updateSelectedVersion(): void {
        this.currentVersion = this.versionsById.get(this.currentOption.value);
    }

    onBackClicked(): void {
        this.$emit('close');
    }

    private async fetchVersions(): Promise<void> {
        const versions = (await axios.get<EditorVersion[]>('/editors')).data;
        if (!versions.length) {
            this.noVersions = true;
            this.options = [NO_VERSIONS_OPTION];
            this.currentOption = NO_VERSIONS_OPTION;
            this.versionsById = new Map([[EMPTY_VERSION.id, EMPTY_VERSION]]);
            this.currentVersion = EMPTY_VERSION;
            return;
        }

        this.options = versions.map((version) => {
            return {
                label: `Version ${version.id} (${new Date(
                    version.lastModified
                ).toLocaleDateString()})`,
                value: version.id,
            };
        });

        this.versionsById = new Map(
            versions.map((version) => [version.id, version])
        );

        this.currentOption = this.options[0];
        this.currentVersion = versions[0];
    }
}
</script>

<style scoped lang="scss">
.unavailable {
    color: gray;
    opacity: 0.8;
}
.available {
    background-color: transparent;
    border-radius: 0.5em;
    transition: background-color 0.2s;

    &:hover {
        background-color: rgba(125, 125, 125, 0.2);
    }
}
</style>
