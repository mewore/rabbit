<template>
    <div id="frame-analysis">
        <div class="top-part">
            <div class="left-part" v-if="hasLeftPart">
                <div v-for="message in messageItems" :key="message.index">
                    <q-btn-group v-if="message.buttons.length" rounded>
                        <q-btn
                            v-for="button in message.buttons"
                            :key="button.index"
                            :icon="button.icon"
                            color="purple-4"
                            size="sm"
                            rounded
                            @click="button.action"
                        >
                            <q-tooltip v-text="button.tooltip"></q-tooltip>
                        </q-btn>
                    </q-btn-group>
                    <span v-text="message.text"></span>
                </div>
            </div>
            <div class="right-part">
                <div class="frame-id-label">
                    #{{ frames[frameIndex].frameId }}
                </div>
                <img id="frame-analysis-preview" :src="frameImageData" />
            </div>
        </div>
        <div class="bottom-part">
            <q-slider
                v-model="frameIndex"
                :min="0"
                :max="lastFrameIndex"
                :step="1"
                :markers="1"
                label
                :label-value="`#${frameIndex + 1}/${lastFrameIndex + 1}`"
                @update:model-value="onFrameIndexUpdated()"
                color="purple-4"
            />
        </div>
    </div>
</template>

<script lang="ts">
import { QBtn, QBtnGroup, QSlider, QTooltip } from 'quasar';
import { Options, Vue } from 'vue-class-component';

import { FrameInfo, FrameMessageAttachment } from '@/game/debug/frame-info';

interface MessageItem {
    index: number;
    buttons: MessageButton[];
    text: string;
}

interface MessageButton {
    index: number;
    icon: string;
    tooltip: string;
    action: () => void;
}

function attachmentToButton(
    attachment: FrameMessageAttachment,
    index: number
): MessageButton {
    return {
        index,
        action: () => window.console.log(attachment.reference),
        // action: window.console.log.bind(window.console, attachment.reference),
        icon: attachment.icon,
        tooltip: attachment.tooltip,
    };
}

@Options({
    components: {
        QBtn,
        QBtnGroup,
        QSlider,
        QTooltip,
    },
    props: {
        frames: Array,
    },
})
export default class FrameAnalysisMenu extends Vue {
    frameIndex = 0;
    frames!: FrameInfo[];
    hasLeftPart = true;
    lastFrameIndex = 0;

    frameImageData = '';
    messageItems: MessageItem[] = [];

    mounted(): void {
        this.lastFrameIndex = this.frames.length - 1;
        this.hasLeftPart = this.frames.some(
            (frame) => frame.messages.length > 0
        );
        this.onFrameIndexUpdated();
    }

    onFrameIndexUpdated(): void {
        const frame = this.frames[this.frameIndex];
        this.frameImageData = frame.imageData;
        this.messageItems = frame.messages.map((message, index) => ({
            index,
            text: message.text,
            buttons: message.attachments
                ? message.attachments.map(attachmentToButton)
                : [],
        }));
    }
}
</script>

<style scoped lang="scss">
#frame-analysis {
    // font-family: sans-serif;
    height: 60vh;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .bottom-part {
        margin: 0 2em;
    }

    .top-part {
        display: flex;
        max-width: 100%;
        flex: 1;
        overflow: hidden;
        justify-content: center;

        .left-part {
            min-width: 50%;
            flex: 1;
            overflow-y: auto;
            white-space: pre-wrap;
            text-align: left;

            > div {
                transition: background-color 50ms;
                &:hover {
                    background-color: rgba(125, 125, 125, 0.5);
                }
            }

            .q-btn-group {
                margin-right: 0.3em;
            }
        }

        #frame-analysis-preview {
            max-width: 100%;
            max-height: 100%;
        }
    }
}
</style>
