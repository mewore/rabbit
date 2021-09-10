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
        <div>FPS: {{ lastSegment.fps }} / {{ lastSegment.targetFps }}</div>
        <div>Draw calls per frame: {{ lastSegment.drawCallsPerFrame }}</div>
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
            default: 0.05,
            type: Number,
        },
        smoothness: {
            default: 1.0,
            type: Number,
        },
    },
})
export default class PerformanceDisplay extends Vue {
    segmentTime!: number;
    smoothness!: number;

    private index = 0;
    private readonly resolution = 150;

    smoothnessCount = Math.min(
        this.resolution - 1,
        this.smoothness / this.segmentTime
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
            const targetFps = Math.round(
                averageTargetFrameCount / this.segmentTime
            );
            const fps = Math.round(averageFrameCount / this.segmentTime);

            this.segments[this.index++] = {
                fps: Math.min(fps, targetFps),
                targetFps,
                className: this.getSegmentClass(fps, targetFps),
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
            &.great {
                background: cyan;
            }
            &.good {
                background: greenyellow;
            }
            &.medium {
                background: orange;
            }
            &.bad {
                background: indianred;
            }
            &.horrible {
                background: red;
            }
            &.skipped {
                visibility: hidden;
            }
            &.empty {
                visibility: hidden;
            }
        }
    }
}
</style>
