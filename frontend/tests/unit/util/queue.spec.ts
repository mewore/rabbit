import { beforeEach, describe, expect, it } from '@jest/globals';

import { ArrayQueue, Queue } from '@/util/queue';

describe('ArrayQueue', () => {
    let queue: Queue<string>;

    beforeEach(() => {
        queue = new ArrayQueue<string>();
        queue.push('first');
        queue.push('second');
        queue.pop();
        queue.pop();
        queue.push('first');
        queue.push('second');
        queue.push('third');
    });

    describe('iterating through all elements', () => {
        it('should yield the same elements that have been inserted in the same order', () => {
            expect(Array.from(queue)).toEqual(['first', 'second', 'third']);
        });
    });

    describe('length', () => {
        it('should be equal to the number of elements', () => {
            expect(queue).toHaveLength(3);
        });
    });

    describe('front', () => {
        it('should be the oldest added element', () => {
            expect(queue.front).toBe('first');
        });
    });

    describe('clone', () => {
        it('should create an independent queue', () => {
            const cloned = queue.clone();
            cloned.pop();
            expect(Array.from(cloned)).toEqual(['second', 'third']);
            expect(Array.from(queue)).toEqual(['first', 'second', 'third']);
        });
    });

    describe('push', () => {
        it('should add it before the element at the back', () => {
            queue.push('fourth');
            expect(Array.from(queue)).toEqual(['first', 'second', 'third', 'fourth']);
        });
    });

    describe('pop', () => {
        it('should remove and return the front element', () => {
            const result = queue.pop();
            expect(result).toBe('first');
            expect(Array.from(queue)).toEqual(['second', 'third']);
        });
    });

    describe('popWhile', () => {
        it('should remove the front elements which match the predicate', () => {
            queue.popWhile((element) => element.startsWith('f') || element.startsWith('t'));
            expect(Array.from(queue)).toEqual(['second', 'third']);
        });
    });

    describe('removeIf', () => {
        it('should remove the elements which match the predicate', () => {
            queue.removeIf((element) => element.startsWith('f') || element.startsWith('t'));
            expect(Array.from(queue)).toEqual(['second']);
        });
    });
});
