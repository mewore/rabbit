import Ammo from 'ammo.js';
import { AdditiveBlending, BufferAttribute, BufferGeometry, LineBasicMaterial, LineSegments } from 'three';

import { BulletDebugDrawModes } from '../physics/bullet-debug-draw-modes';
import { RenderAware } from '../util/render-aware';
import { FrameAnalysis } from './frame-analysis';

/**
 * Creates THREE.js wireframe previews of all Ammo.js bodies.
 *
 * [NOTE] There already is a JS implemenation of this: https://github.com/InfiniteLee/ammo-debug-drawer
 * However, I found it when I was almost done implementing this myself. It did give me a valuable hint, though -
 * ammojs-typed had misled me into thinking that an actual btVector3 is passed to {@link drawLine};
 * instead, it's the index of the first byte of the actual btVector3 in {@code Ammo.HEAPF32}
 * (which should also be divided by 4 because {@code float}s consist of 4 bytes).
 */
export class AmmoDebugRenderer extends LineSegments<BufferGeometry, LineBasicMaterial> implements RenderAware {
    readonly drawer = new Ammo.DebugDrawer();

    private debugMode =
        BulletDebugDrawModes.DRAW_WIREFRAME |
        BulletDebugDrawModes.DRAW_AABB |
        BulletDebugDrawModes.DRAW_CONTACT_POINTS |
        BulletDebugDrawModes.DRAW_NORMALS |
        BulletDebugDrawModes.DRAW_TEXT;

    private points: Float32Array = new Float32Array();
    private pointColors: Float32Array = new Float32Array();
    private pointCount = 0;

    constructor(private readonly frameAnalysis: FrameAnalysis) {
        super();
        this.geometry = new BufferGeometry();
        this.material = new LineBasicMaterial({
            fog: false,
            vertexColors: true,
            blending: AdditiveBlending,
        });

        this.drawer.draw3dText = this.draw3dText.bind(this);
        this.drawer.drawContactPoint = this.drawContactPoint.bind(this);
        this.drawer.drawLine = this.drawLine.bind(this);
        this.drawer.getDebugMode = this.getDebugMode.bind(this);
        this.drawer.setDebugMode = this.setDebugMode.bind(this);
        this.drawer.reportErrorWarning = this.reportErrorWarning.bind(this);
    }

    clearGeometry(): void {
        this.pointCount = 0;
    }

    private expandArray(array: Float32Array, newLength: number) {
        const result = new Float32Array(newLength);
        for (let i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    private drawLineWithCoordinates(
        fromX: number,
        fromY: number,
        fromZ: number,
        toX: number,
        toY: number,
        toZ: number,
        color: unknown
    ): void {
        let index = this.pointCount * 3;
        const lastIndex = index + 5;
        let newLength = Math.max(this.points.length, 1);
        while (lastIndex >= newLength) {
            newLength *= 2;
        }
        if (newLength > this.points.length) {
            this.points = this.expandArray(this.points, newLength);
            this.pointColors = this.expandArray(this.pointColors, newLength);
        }

        const colorIndex = (color as number) / 4;
        const heap = Ammo.HEAPF32;

        for (let i = 0; i < 3; i++) {
            this.pointColors[index + i] = this.pointColors[index + i + 3] = heap[colorIndex + i];
        }

        this.points[index++] = fromX;
        this.points[index++] = fromY;
        this.points[index++] = fromZ;
        this.points[index++] = toX;
        this.points[index++] = toY;
        this.points[index++] = toZ;
        this.pointCount += 2;
    }

    private drawLine(from: unknown, to: unknown, color: unknown): void {
        const fHeap = Ammo.HEAPF32;
        let a = (from as number) / 4;
        let b = (to as number) / 4;
        this.drawLineWithCoordinates(fHeap[a++], fHeap[a++], fHeap[a++], fHeap[b++], fHeap[b++], fHeap[b++], color);
    }

    private drawContactPoint(
        pointOnB: unknown,
        normalOnB: unknown,
        distance: number,
        _lifeTime: number,
        color: unknown
    ): void {
        const fHeap = Ammo.HEAPF32;
        let pointIndex = (pointOnB as number) / 4;
        let normalIndex = (normalOnB as number) / 4;
        this.drawLineWithCoordinates(
            fHeap[pointIndex],
            fHeap[pointIndex + 1],
            fHeap[pointIndex + 2],
            fHeap[pointIndex++] + fHeap[normalIndex++] * (distance + 1),
            fHeap[pointIndex++] + fHeap[normalIndex++] * (distance + 1),
            fHeap[pointIndex++] + fHeap[normalIndex++] * (distance + 1),
            color
        );
    }

    private reportErrorWarning(warningString: string): void {
        if (this.frameAnalysis.analyzing) {
            this.frameAnalysis.addMessage(
                '[AmmoDebugRenderer] Error or warning encountered by Ammo.js: ' + warningString
            );
        }
    }

    private draw3dText(location: unknown, textString: string): void {
        const heap = Ammo.HEAPF32;
        if (this.frameAnalysis.analyzing) {
            const index = (location as number) / 4;
            this.frameAnalysis.addMessage(
                '[AmmoDebugRenderer] Text at (',
                heap[index],
                heap[index + 1],
                heap[index + 2],
                `): ${textString}`
            );
        }
    }

    private setDebugMode(debugMode: number): void {
        this.debugMode = debugMode;
    }

    private getDebugMode(): number {
        return this.debugMode;
    }

    longBeforeRender(): void {}

    beforeRender(): void {
        const pointAttribute = new BufferAttribute(this.points, 3);
        const colorAttribute = new BufferAttribute(this.pointColors, 3);
        pointAttribute.count = colorAttribute.count = this.pointCount;
        this.geometry.setAttribute('position', pointAttribute);
        this.geometry.setAttribute('color', colorAttribute);
        this.geometry.computeBoundingSphere();
    }
}
