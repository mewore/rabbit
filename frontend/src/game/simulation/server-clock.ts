import { ArrayQueue, Queue } from '@/util/queue';

const OFFSET_GUESS_LIMIT = 50;

export class ServerClock {
    private offset = 0;

    private offsetSum = 0;

    private readonly offsetGuesses: Queue<number> = new ArrayQueue<number>();

    guessServerTime(now: number, serverTimestamp: number, latency: number): number {
        // The server deliberately delays the states it sends by 1.5x the player latency
        // So, the time in the server right now should be not just [serverTimestamp + latency],
        // but [[serverTimestamp + latency] + latency * 1.5].
        // See https://github.com/mewore/rabbit/issues/100
        const offsetGuess = serverTimestamp + latency * 2.5 - now;

        this.offsetSum += offsetGuess;
        if (this.offsetGuesses.length >= OFFSET_GUESS_LIMIT) {
            this.offsetSum -= this.offsetGuesses.pop() || 0;
        }
        this.offsetGuesses.push(offsetGuess);
        this.offset = this.offsetSum / this.offsetGuesses.length;
        return this.localTimeToServerTime(now);
    }

    localTimeToServerTime(timestamp: number): number {
        return timestamp + this.offset;
    }
}
