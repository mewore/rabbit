import { beforeEach, describe, expect, it } from '@jest/globals';

import { FrameAnalysis } from '@/game/debug/frame-analysis';
import { FrameInfo, FrameMessage, FrameMessageAttachment, WorldUpdateState } from '@/game/debug/frame-info';

function createFakeCanvas(toDataUrlResult = ''): HTMLCanvasElement {
    return {
        toDataURL: (format?: string, quality?: number) => (format ? `${format}(${quality}):` : '') + toDataUrlResult,
    } as HTMLCanvasElement;
}

describe('FrameAnalysis', () => {
    let frameAnalysis: FrameAnalysis;

    beforeEach(() => {
        frameAnalysis = new FrameAnalysis();
    });

    describe('initially', () => {
        it('should not be analyzing', () => {
            expect(frameAnalysis.analyzing).toBe(false);
        });
    });

    describe('#start', () => {
        beforeEach(() => {
            frameAnalysis.start();
        });

        it('should mark it as analyzing', () => {
            expect(frameAnalysis.analyzing).toBe(true);
        });

        describe('when already analyzing', () => {
            it('should throw an error', () => {
                expect(() => frameAnalysis.start()).toThrow('Cannot start an analysis while analyzing!');
            });
        });
    });

    describe('#addMessage', () => {
        describe('when analyzing', () => {
            beforeEach(() => {
                frameAnalysis.start();
            });

            it('should add a message only to the following frame', () => {
                frameAnalysis.addMessage('first');
                frameAnalysis.addMessage('second');
                frameAnalysis.captureFrame(createFakeCanvas());
                frameAnalysis.captureFrame(createFakeCanvas());
                frameAnalysis.addMessage('third');
                frameAnalysis.captureFrame(createFakeCanvas());

                const result = frameAnalysis.complete();
                expect(result.map((frame) => frame.messages)).toEqual([
                    [{ text: 'first' }, { text: 'second' }],
                    [],
                    [{ text: 'third' }],
                ] as FrameMessage[][]);
            });

            describe('when called with attachments', () => {
                it('should include a reference to the attachments', () => {
                    const attachments: FrameMessageAttachment[] = [
                        { icon: 'first-icon', reference: { a: 1 }, tooltip: 'first-tooltip' },
                        { icon: 'second-icon', reference: [{ b: 2 }], tooltip: 'second-tooltip' },
                    ];
                    frameAnalysis.addMessage({ text: '', attachments });
                    frameAnalysis.captureFrame(createFakeCanvas());

                    const result = frameAnalysis.complete();
                    expect(result).toHaveLength(1);
                    expect(result[0].messages).toHaveLength(1);
                    expect(result[0].messages[0].attachments).toBe(attachments);
                });
            });
        });

        describe('when not analyzing', () => {
            it('should throw an error', () => {
                expect(() => frameAnalysis.addMessage('a')).toThrow('Cannot add a message while not analyzing!');
            });
        });
    });

    describe('#captureFrame', () => {
        describe('when analyzing', () => {
            beforeEach(() => {
                frameAnalysis.start();
            });

            it('should add a frame for each call', () => {
                frameAnalysis.captureFrame(createFakeCanvas());
                frameAnalysis.captureFrame(createFakeCanvas());
                expect(frameAnalysis.complete()).toHaveLength(2);
            });

            describe('when a pending world update state has been set', () => {
                beforeEach(() => {
                    frameAnalysis.pendingWorldUpdateState = WorldUpdateState.ACCEPTED;
                });
                it('should save the world update state in the resulting frame', () => {
                    frameAnalysis.captureFrame(createFakeCanvas('frame'));
                    expect(frameAnalysis.complete()[0].worldUpdateState).toBe(WorldUpdateState.ACCEPTED);
                });

                it('should NOT save the world update state in later frames', () => {
                    frameAnalysis.captureFrame(createFakeCanvas('frame'));
                    frameAnalysis.captureFrame(createFakeCanvas('frame'));
                    expect(frameAnalysis.complete()[1].worldUpdateState).not.toBe(WorldUpdateState.ACCEPTED);
                });
            });

            describe('when with a quality of 1', () => {
                it('should create a default-encoded frame image', () => {
                    frameAnalysis.captureFrame(createFakeCanvas('frame'));
                    expect(frameAnalysis.complete()[0].imageData).toBe('frame');
                });
            });

            describe('when with a quality below 1', () => {
                beforeEach(() => {
                    frameAnalysis.imageQuality = 0.5;
                });

                it('should create a JPEG-encoded frame image with the specified quality', () => {
                    frameAnalysis.captureFrame(createFakeCanvas('frame'));
                    expect(frameAnalysis.complete()[0].imageData).toBe('image/jpeg(0.5):frame');
                });
            });
        });

        describe('when not analyzing', () => {
            it('should throw an error', () => {
                expect(() => frameAnalysis.captureFrame(createFakeCanvas())).toThrow(
                    'Cannot capture a frame while not analyzing!'
                );
            });
        });
    });

    describe('#complete', () => {
        describe('when analyzing', () => {
            beforeEach(() => {
                frameAnalysis.start();
            });

            it('should return an array of the frames', () => {
                frameAnalysis.captureFrame(createFakeCanvas());
                frameAnalysis.captureFrame(createFakeCanvas());
                expect(frameAnalysis.complete()).toEqual([
                    { frameId: 1, imageData: '', messages: [], worldUpdateState: undefined },
                    { frameId: 2, imageData: '', messages: [], worldUpdateState: undefined },
                ] as FrameInfo[]);
            });

            it('should stop the analysis', () => {
                frameAnalysis.complete();
                expect(frameAnalysis.analyzing).toBe(false);
            });
        });

        describe('when analyzing after a previous analysis', () => {
            beforeEach(() => {
                frameAnalysis.start();
                frameAnalysis.captureFrame(createFakeCanvas('first'));
                frameAnalysis.complete();
                frameAnalysis.start();
            });

            it('should include only the new frames', () => {
                frameAnalysis.captureFrame(createFakeCanvas('second'));
                expect(frameAnalysis.complete()).toEqual([
                    { frameId: 1, imageData: 'second', messages: [], worldUpdateState: undefined },
                ] as FrameInfo[]);
            });
        });

        describe('when not analyzing', () => {
            it('should throw an error', () => {
                expect(() => frameAnalysis.complete()).toThrow('Cannot complete an analysis while not analyzing!');
            });
        });
    });
});
