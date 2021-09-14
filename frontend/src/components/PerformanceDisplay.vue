<template>
    <div id="performance-display">
        <div class="frame-info-container">
            <div
                v-for="segment in segments"
                :key="segment"
                :class="segment.className"
                :style="{ opacity: segment.opacity }"
            ></div>
        </div>
        <div class="detailed-info">
            <div>FPS: {{ lastSegment.fps }} / {{ lastSegment.targetFps }}</div>
            <div>
                Milliseconds per frame:
                {{
                    lastSegment.msPerFrame === Infinity
                        ? ''
                        : lastSegment.msPerFrame
                }}ms (target: {{ lastSegment.targetMsPerFrame }}ms)
            </div>
            <div>Draw calls per frame: {{ lastSegment.drawCallsPerFrame }}</div>
        </div>
    </div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';

interface Segment {
    inactive?: boolean;
}

interface SegmentInfo extends Segment {
    frameCount: number;
    targetFrameCount: number;
    drawCalls: number;
    fps: number;
    targetFps: number;
    msPerFrame: number;
    targetMsPerFrame: number;
    drawCallsPerFrame: number;
    className: string;
    opacity?: number;
}

interface EmptySegment extends Segment {
    className: 'empty';
}

@Options({
    props: {
        segmentTime: {
            default: 0.5,
            type: Number,
        },
        smoothnessTime: {
            default: 3.0,
            type: Number,
        },
    },
})
export default class PerformanceDisplay extends Vue {
    segmentTime!: number;
    smoothnessTime!: number;

    private index = 0;
    private readonly resolution = 50;

    smoothnessCount = Math.min(
        this.resolution - 1,
        this.smoothnessTime / this.segmentTime
    );

    private segmentEnd = 0;
    private lastFrameTime = 0;
    private frameCount = 0;
    private targetFrameCount = 0;
    private segmentDrawCalls = 0;

    lastSegment: EmptySegment | SegmentInfo = { className: 'empty' };
    readonly segments: (EmptySegment | SegmentInfo)[] = [];

    beforeCreate(): void {
        for (let i = 0; i < this.resolution; i++) {
            this.segments.push({ className: 'empty' });
        }
        this.lastSegment = this.segments[0];
    }

    start(timestamp: number): void {
        this.segmentEnd = timestamp + this.segmentTime;
        this.frameCount = 0;
        this.lastFrameTime = timestamp;
    }

    registerFrame(
        timestamp: number,
        targetDelta: number,
        drawCalls: number,
        skipped = false
    ): void {
        const targetFrameCount = this.segmentTime / targetDelta;
        while (timestamp > this.segmentEnd) {
            this.targetFrameCount +=
                (this.segmentEnd - this.lastFrameTime) * targetFrameCount;
            this.targetFrameCount /= this.segmentTime;
            const lastIndex =
                this.index === 0 ? this.segments.length - 1 : this.index - 1;
            const lastSegment = this.segments[lastIndex];
            if (this.segmentIsVisible(lastSegment)) {
                this.segments[lastIndex] = {
                    ...lastSegment,
                    opacity: 0.6,
                };
            }

            const gradientSize = 3;
            for (
                let i = 0, nextIndex = (this.index + 1) % this.segments.length;
                i < gradientSize;
                i++, nextIndex = (nextIndex + 1) % this.segments.length
            ) {
                const nextSegment = this.segments[nextIndex];
                if (this.segmentIsVisible(nextSegment)) {
                    this.segments[nextIndex] = {
                        ...nextSegment,
                        opacity: 0.6 * (i / gradientSize),
                    };
                }
            }
            let segmentsTakenIntoAccount = 1;
            let averageFrameCount = this.frameCount;
            let averageTargetFrameCount = this.targetFrameCount;
            let averageDrawCalls = this.segmentDrawCalls;
            for (
                let i = 0, prevIndex = this.index - 1;
                i < this.smoothnessCount;
                i++, prevIndex--
            ) {
                const prevSegment =
                    this.segments[
                        prevIndex < 0
                            ? prevIndex + this.segments.length
                            : prevIndex
                    ];
                if (!this.segmentIsVisible(prevSegment)) {
                    break;
                }
                averageFrameCount += prevSegment.frameCount;
                averageTargetFrameCount += prevSegment.targetFrameCount;
                averageDrawCalls += prevSegment.drawCalls;
                segmentsTakenIntoAccount++;
            }
            averageFrameCount /= segmentsTakenIntoAccount;
            averageTargetFrameCount /= segmentsTakenIntoAccount;
            averageDrawCalls /= segmentsTakenIntoAccount;
            const fps = averageFrameCount / this.segmentTime;
            const roundedFps = Math.round(fps);
            const roundedMsPerFrame = Math.round(1000 / roundedFps);

            const targetFps = averageTargetFrameCount / this.segmentTime;
            const roundedTargetFps = Math.round(
                averageTargetFrameCount / this.segmentTime
            );
            const roundedTargetMsPerFrame = Math.round(1000 / roundedTargetFps);

            this.segments[this.index++] = {
                fps: Math.min(roundedFps, roundedTargetFps),
                targetFps: roundedTargetFps,
                msPerFrame: roundedMsPerFrame,
                targetMsPerFrame: roundedTargetMsPerFrame,
                className: this.getSegmentClass(fps + 1, targetFps),
                drawCalls: this.segmentDrawCalls,
                drawCallsPerFrame: Math.round(
                    averageDrawCalls / averageFrameCount
                ),
                frameCount: this.frameCount,
                targetFrameCount: this.targetFrameCount,
            };
            this.lastSegment = this.segments[this.index - 1];

            if (this.index >= this.segments.length) {
                this.index = 0;
            }
            this.segmentEnd += this.segmentTime;
            this.lastFrameTime = this.segmentEnd - this.segmentTime;
            this.frameCount = 0;
            this.targetFrameCount = 0;
            this.segmentDrawCalls = 0;
        }
        if (!skipped) {
            this.frameCount++;
            this.segmentDrawCalls += drawCalls;
        }
        this.targetFrameCount +=
            (timestamp - this.lastFrameTime) * targetFrameCount;
        this.lastFrameTime = timestamp;
    }

    private segmentIsVisible(segment: Segment): segment is SegmentInfo {
        return (segment as unknown as SegmentInfo).fps != null;
    }

    getSegmentClass(fps: number, targetFps: number): string {
        if (fps >= targetFps) {
            return 'perfect';
        }
        if (fps >= targetFps * 0.8) {
            return 'great';
        }
        if (fps >= targetFps * 0.6) {
            return 'good';
        }
        if (fps >= targetFps * 0.4) {
            return 'medium';
        }
        if (fps >= targetFps * 0.2) {
            return 'bad';
        }
        return 'horrible';
    }
}
</script>

<style scoped lang="scss">
#performance-display {
    color: #ddd;
    padding: 3px;
    font-family: sans-serif;
    min-width: 10em;
    position: absolute;
    top: 5px;
    right: 5px;
    display: flex;
    .frame-info-container {
        width: 20em;
        height: 25px;
        opacity: 0.8;
        display: flex;
        > div {
            display: inline-block;
            flex: 1;
            &.perfect {
                background: hsl(180, 60%, 70%);
            }
            &.great {
                background: hsl(135, 60%, 70%);
            }
            &.good {
                background: hsl(75, 60%, 60%);
            }
            &.medium {
                background: hsl(50, 80%, 50%);
            }
            &.bad {
                background: hsl(25, 80%, 50%);
            }
            &.horrible {
                background: hsl(0, 100%, 50%);
            }
            &.skipped {
                visibility: hidden;
            }
            &.empty {
                visibility: hidden;
            }
        }
    }
    .detailed-info {
        font-size: 0.9em;
    }
}
</style>
