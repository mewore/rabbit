import { beforeEach, describe, expect, it } from '@jest/globals';

import { ArrayDeque, Deque } from '@/util/deque';

describe('ArrayDeque', () => {
    let deque: Deque<string>;

    beforeEach(() => {
        deque = new ArrayDeque<string>();
        deque.push('first');
        deque.push('second');
        deque.pop();
        deque.pop();
        deque.push('first');
        deque.push('second');
        deque.push('third');
    });

    describe('back', () => {
        it('should be the most recently added element', () => {
            expect(deque.back).toBe('third');
        });
    });

    describe('pushFront', () => {
        it('should add it before the front element', () => {
            deque.pushFront('zeroth');
            expect(Array.from(deque)).toEqual(['zeroth', 'first', 'second', 'third']);
        });
    });

    describe('popBack', () => {
        it('should remove and return the element at the back', () => {
            const result = deque.popBack();
            expect(result).toBe('third');
            expect(Array.from(deque)).toEqual(['first', 'second']);
        });
    });
});
