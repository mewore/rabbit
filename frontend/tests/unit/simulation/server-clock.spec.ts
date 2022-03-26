import { expect } from 'chai';

import { ServerClock } from '@/game/simulation/server-clock';

describe('ServerClock', () => {
    const SERVER_TIME = 2;
    const LATENCY = 1;

    describe('when given a single data point', () => {
        it('should make a correct prediction', () => {
            const clock = new ServerClock();
            expect(clock.guessServerTime(10, SERVER_TIME, LATENCY)).to.approximately(
                SERVER_TIME + LATENCY * 2.5,
                0.00001
            );
        });
    });

    describe('when given two data points', () => {
        it('should make an average prediction', () => {
            const clock = new ServerClock();
            clock.guessServerTime(10, 2, LATENCY);
            clock.guessServerTime(20, 14, LATENCY);
            expect(clock.localTimeToServerTime(20)).to.approximately(13 + LATENCY * 2.5, 0.00001);
        });
    });

    describe('when given 100 data points, only the last one of which is slightly different', () => {
        it('should make a very close prediction regardless', () => {
            const clock = new ServerClock();
            for (let i = 0; i < 99; i++) {
                clock.guessServerTime(i, i + SERVER_TIME, LATENCY);
            }
            clock.guessServerTime(-5, -5, LATENCY);
            expect(clock.localTimeToServerTime(10)).to.approximately(10 + SERVER_TIME + LATENCY * 2.5, 0.1);
        });
    });
});
