import { expect } from 'chai';

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
            expect(Array.from(queue)).to.deep.equal(['first', 'second', 'third']);
        });
    });

    describe('length', () => {
        it('should be equal to the number of elements', () => {
            expect(queue.length).to.equal(3);
        });
    });

    describe('front', () => {
        it('should be the oldest added element', () => {
            expect(queue.front).to.equal('first');
        });
    });

    describe('clone', () => {
        it('should create an independent queue', () => {
            const cloned = queue.clone();
            cloned.pop();
            expect(Array.from(cloned)).to.deep.equal(['second', 'third']);
            expect(Array.from(queue)).to.deep.equal(['first', 'second', 'third']);
        });
    });

    describe('push', () => {
        it('should add it before the element at the back', () => {
            queue.push('fourth');
            expect(Array.from(queue)).to.deep.equal(['first', 'second', 'third', 'fourth']);
        });
    });

    describe('pop', () => {
        it('should remove and return the front element', () => {
            const result = queue.pop();
            expect(result).to.equal('first');
            expect(Array.from(queue)).to.deep.equal(['second', 'third']);
        });
    });

    describe('popWhile', () => {
        it('should remove the front elements which match the predicate', () => {
            queue.popWhile((element) => element.startsWith('f') || element.startsWith('t'));
            expect(Array.from(queue)).to.deep.equal(['second', 'third']);
        });
    });

    describe('removeIf', () => {
        it('should remove the elements which match the predicate', () => {
            queue.removeIf((element) => element.startsWith('f') || element.startsWith('t'));
            expect(Array.from(queue)).to.deep.equal(['second']);
        });
    });
});
