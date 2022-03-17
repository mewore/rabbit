export interface Queue<T> extends Iterable<T> {
    /**
     * The current length of the queue.
     */
    readonly length: number;

    /**
     * Get the first element of the queue.
     */
    readonly front: T | undefined;

    /**
     * Remove all items from this queue.
     */
    clear(): void;

    /**
     * Create a duplicate of this queue. The two queues are independent of each other.
     */
    clone(): Queue<T>;

    /**
     * Add an item into the queue.
     * @param value The item value.
     */
    push(value: T): void;

    /**
     * Get the first element of the queue. vRemove it.
     */
    pop(): T | undefined;

    /**
     * Remove elements from the front until a certain condition is met.
     * @param predicate A function which returns `true` for the element at which the removal should stop.
     */
    popWhile(predicate: (value: T) => boolean): T[];

    /**
     * Do an in-place removal of elements.
     * @param predicate A function which returns `true` for all elements which should be removed.
     */
    removeIf(predicate: (value: T) => boolean): void;
}

export class ArrayQueue<T> implements Queue<T> {
    protected frontIndex = 0;
    protected currentLength = 0;
    protected buffer = Array.from<T>({ length: 1 });

    [Symbol.iterator](): Iterator<T> {
        let index = this.frontIndex;
        let i = 0;
        return {
            next: () => {
                if (i >= this.currentLength) {
                    return { done: true, value: undefined };
                }
                const result = this.buffer[index];
                index = ++index & (this.buffer.length - 1);
                ++i;
                return { done: false, value: result };
            },
        };
    }

    get length(): number {
        return this.currentLength;
    }

    get front(): T | undefined {
        return this.currentLength > 0 ? this.buffer[this.frontIndex] : undefined;
    }

    clear(): void {
        this.currentLength = 0;
    }

    clone(): Queue<T> {
        const newQueue = new ArrayQueue<T>();
        newQueue.buffer = this.buffer.slice();
        newQueue.frontIndex = this.frontIndex;
        newQueue.currentLength = this.currentLength;
        return newQueue;
    }

    push(value: T): void {
        if (this.currentLength >= this.buffer.length) {
            this.expand();
        }
        // The buffer length is a power of two so [&] is the same as the much more expensive [%] operation
        this.buffer[(this.frontIndex + this.currentLength) & (this.buffer.length - 1)] = value;
        ++this.currentLength;
    }

    pop(): T | undefined {
        if (this.currentLength <= 0) {
            return undefined;
        }
        const result = this.front;
        this.frontIndex = ++this.frontIndex & (this.buffer.length - 1);
        --this.currentLength;
        return result;
    }

    popWhile(predicate: (value: T) => boolean): T[] {
        const bitmask = this.buffer.length - 1;
        const result: T[] = [];
        while (this.currentLength > 0 && predicate(this.buffer[this.frontIndex])) {
            result.push(this.buffer[this.frontIndex]);
            ++this.frontIndex;
            this.frontIndex &= bitmask;
            --this.currentLength;
        }
        return result;
    }

    removeIf(predicate: (value: T) => boolean): void {
        let newLength = 0;
        const bitmask = this.buffer.length - 1;
        let currentValue: T;
        for (let i = 0; i < this.currentLength; i++) {
            currentValue = this.buffer[(this.frontIndex + i) & bitmask];
            if (!predicate(currentValue)) {
                this.buffer[(this.frontIndex + newLength) & bitmask] = currentValue;
                ++newLength;
            }
        }
        this.currentLength = newLength;
    }

    protected expand(): void {
        const newBuffer: T[] = Array.from({ length: this.buffer.length * 2 });
        let index = -1;
        for (const element of this) {
            newBuffer[++index] = element;
        }
        this.buffer = newBuffer;
        this.frontIndex = 0;
    }
}
