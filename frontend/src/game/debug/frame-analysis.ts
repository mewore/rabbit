import { ArrayQueue } from '@/util/queue';

import { FrameInfo, FrameMessage, WorldUpdateState } from './frame-info';

type Vector3 = { readonly x: number; readonly y: number; readonly z: number };
type AmmoVector3 = { x(): number; y(): number; z(): number };

type Vector2 = { readonly x: number; readonly y: number };
type AmmoVector2 = { x(): number; y(): number };

type VectorLike = Vector2 | Vector3 | AmmoVector2 | AmmoVector3;

function round(num: number): number {
    return Math.round(num * 100) / 100;
}

function roundVec2(vec2: Vector2): string {
    return `(${round(vec2.x)}, ${round(vec2.y)})`;
}

function roundVec3(vec3: Vector3): string {
    return `(${round(vec3.x)}, ${round(vec3.y)}, ${round(vec3.z)})`;
}

function roundAmmoVec3(vec3: AmmoVector3): string {
    return `(${round(vec3.x())}, ${round(vec3.y())}, ${round(vec3.z())})`;
}

function roundAmmoVec2(vec2: AmmoVector2): string {
    return `(${round(vec2.x())}, ${round(vec2.y())})`;
}

function isAmmoVec2(vec2: unknown): vec2 is AmmoVector2 {
    return typeof (vec2 as AmmoVector3).x === 'function' && typeof (vec2 as AmmoVector3).y === 'function';
}

function isAmmoVec3(vec3: unknown): vec3 is AmmoVector3 {
    return isAmmoVec2(vec3) && typeof (vec3 as AmmoVector3).z === 'function';
}

function isVec2(vec3: unknown): vec3 is Vector2 {
    return typeof (vec3 as Vector3).x === 'number' && typeof (vec3 as Vector3).y === 'number';
}
function isVec3(vec3: unknown): vec3 is Vector3 {
    return isVec2(vec3) && typeof (vec3 as Vector3).z === 'number';
}

const FRAME_LIMIT = 300;

/**
 * Per-frame analysis tool for anything that renders on a canvas.
 */
export class FrameAnalysis {
    static GLOBAL = new FrameAnalysis();

    private readonly frames = new ArrayQueue<FrameInfo>();
    private readonly pendingMessages: FrameMessage[] = [];

    private _analyzing = false;

    pendingWorldUpdateState?: WorldUpdateState;
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

    addMessage(
        message: string | FrameMessage,
        ...otherMessageParts: (string | number | boolean | undefined | VectorLike)[]
    ): void {
        if (!this._analyzing) {
            throw new Error('Cannot add a message while not analyzing!');
        }

        const messageSuffix = otherMessageParts
            .map((part) => {
                switch (typeof part) {
                    case 'object':
                        if (isVec2(part)) {
                            return isVec3(part) ? roundVec3(part) : roundVec2(part);
                        } else if (isAmmoVec2(part)) {
                            return isAmmoVec3(part) ? roundAmmoVec3(part) : roundAmmoVec2(part);
                        }
                        return '' + part;
                    case 'number':
                        return round(part);
                    default:
                        return '' + part;
                }
            })
            .join('');
        if (typeof message === 'string') {
            this.pendingMessages.push({ text: message + messageSuffix });
        } else {
            message.text += messageSuffix;
            this.pendingMessages.push(message);
        }
    }

    captureFrame(canvas: HTMLCanvasElement, frameId = -1): void {
        if (!this._analyzing) {
            throw new Error('Cannot capture a frame while not analyzing!');
        }

        while (this.frames.length >= FRAME_LIMIT) {
            this.frames.pop();
        }
        this.frames.push({
            frameId,
            imageData:
                this.imageQuality > 0.99 ? canvas.toDataURL() : canvas.toDataURL('image/jpeg', this.imageQuality),
            messages: this.pendingMessages.splice(0),
            worldUpdateState: this.pendingWorldUpdateState,
        });
        this.pendingWorldUpdateState = undefined;
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
        this.pendingWorldUpdateState = undefined;
    }
}
