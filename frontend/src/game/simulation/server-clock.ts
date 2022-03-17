import { ArrayQueue, Queue } from '@/util/queue';

const OFFSET_GUESS_LIMIT = 50;

export class ServerClock {
    private offset = 0;

    private offsetSum = 0;

    private readonly offsetGuesses: Queue<number> = new ArrayQueue<number>();

    guessServerTime(now: number, serverTimestamp: number, latency: number): number {
        const offsetGuess = serverTimestamp + latency - now;
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
