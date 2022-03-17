import { ArrayQueue } from '@/util/queue';

import { FrameInfo, FrameMessage } from './frame-info';

const FRAME_LIMIT = 300;

/**
 * Per-frame analysis tool for anything that renders on a canvas.
 */
export class FrameAnalysis {
    private readonly frames = new ArrayQueue<FrameInfo>();
    private readonly pendingMessages: FrameMessage[] = [];
    private _analyzing = false;
    imageQuality = 1;

    get analyzing(): boolean {
        return this._analyzing;
    }

    complete(): FrameInfo[] {
        if (!this._analyzing) {
            throw new Error('Cannot complete an analysis while not analyzing!');
        }

        const result = Array.from(this.frames);
        result.forEach((frame, index) => (frame.frameId = frame.frameId === -1 ? index + 1 : frame.frameId));
        this._analyzing = false;
        this.clear();
        return result;
    }

    addMessage(message: string | FrameMessage): void {
        if (!this._analyzing) {
            throw new Error('Cannot add a message while not analyzing!');
        }

        this.pendingMessages.push(typeof message === 'string' ? { text: message } : message);
    }

    captureFrame(canvas: HTMLCanvasElement): void {
        if (!this._analyzing) {
            throw new Error('Cannot capture a frame while not analyzing!');
        }

        while (this.frames.length >= FRAME_LIMIT) {
            this.frames.pop();
        }
        this.frames.push({
            frameId: -1,
            imageData:
                this.imageQuality > 0.99 ? canvas.toDataURL() : canvas.toDataURL('image/jpeg', this.imageQuality),
            messages: this.pendingMessages.splice(0),
        });
    }

    start(): void {
        if (this._analyzing) {
            throw new Error('Cannot start an analysis while analyzing!');
        }

        this.clear();
        this._analyzing = true;
    }

    private clear(): void {
        this.frames.clear();
        this.pendingMessages.splice(0);
    }
}
