import { ArrayQueue, Queue } from './queue';

export interface Deque<T> extends Queue<T> {
    /**
     * Get the last element of the deque.
     */
    readonly back: T | undefined;

    /**
     * Add an item into the deque at the front.
     * @param value The item value.
     */
    pushFront(value: T): void;

    /**
     * Get the last element of the deque. Remove it.
     */
    popBack(): T | undefined;
}

export class ArrayDeque<T> extends ArrayQueue<T> implements Deque<T> {
    get back(): T | undefined {
        return this.currentLength > 0
            ? this.buffer[(this.frontIndex + this.currentLength - 1) & (this.buffer.length - 1)]
            : undefined;
    }

    pushFront(value: T): void {
        if (this.currentLength >= this.buffer.length) {
            this.expand();
        }
        --this.frontIndex;
        if (this.frontIndex < 0) {
            this.frontIndex += this.buffer.length;
        }
        this.buffer[this.frontIndex] = value;
        ++this.currentLength;
    }

    popBack(): T | undefined {
        if (this.currentLength <= 0) {
            return undefined;
        }
        --this.currentLength;
        return this.buffer[(this.frontIndex + this.currentLength) & (this.buffer.length - 1)];
    }
}
