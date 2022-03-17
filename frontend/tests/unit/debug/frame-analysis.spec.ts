import { expect } from 'chai';

import { FrameAnalysis } from '@/game/debug/frame-analysis';
import { FrameInfo, FrameMessage, FrameMessageAttachment } from '@/game/debug/frame-info';

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
            expect(frameAnalysis.analyzing).to.equal(false);
        });
    });

    describe('#start', () => {
        beforeEach(() => {
            frameAnalysis.start();
        });

        it('should mark it as analyzing', () => {
            expect(frameAnalysis.analyzing).to.equal(true);
        });

        describe('when already analyzing', () => {
            it('should throw an error', () => {
                expect(() => frameAnalysis.start()).to.throw('Cannot start an analysis while analyzing!');
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
                expect(result.map((frame) => frame.messages)).to.deep.equal([
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
                    expect(result.length).to.equal(1);
                    expect(result[0].messages.length).to.equal(1);
                    expect(result[0].messages[0].attachments).to.equal(attachments);
                });
            });
        });

        describe('when not analyzing', () => {
            it('should throw an error', () => {
                expect(() => frameAnalysis.addMessage('a')).to.throw('Cannot add a message while not analyzing!');
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
                expect(frameAnalysis.complete().length).to.equal(2);
            });

            describe('when with a quality of 1', () => {
                it('should create a default-encoded frame image', () => {
                    frameAnalysis.captureFrame(createFakeCanvas('frame'));
                    expect(frameAnalysis.complete()[0].imageData).to.equal('frame');
                });
            });

            describe('when with a quality below 1', () => {
                beforeEach(() => {
                    frameAnalysis.imageQuality = 0.5;
                });

                it('should create a JPEG-encoded frame image with the specified quality', () => {
                    frameAnalysis.captureFrame(createFakeCanvas('frame'));
                    expect(frameAnalysis.complete()[0].imageData).to.equal('image/jpeg(0.5):frame');
                });
            });
        });

        describe('when not analyzing', () => {
            it('should throw an error', () => {
                expect(() => frameAnalysis.captureFrame(createFakeCanvas())).to.throw(
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
                expect(frameAnalysis.complete()).to.deep.equal([
                    { frameId: 1, imageData: '', messages: [] },
                    { frameId: 2, imageData: '', messages: [] },
                ] as FrameInfo[]);
            });

            it('should stop the analysis', () => {
                frameAnalysis.complete();
                expect(frameAnalysis.analyzing).to.equal(false);
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
                expect(frameAnalysis.complete()).to.deep.equal([
                    { frameId: 1, imageData: 'second', messages: [] },
                ] as FrameInfo[]);
            });
        });

        describe('when not analyzing', () => {
            it('should throw an error', () => {
                expect(() => frameAnalysis.complete()).to.throw('Cannot complete an analysis while not analyzing!');
            });
        });
    });
});