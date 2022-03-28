import { BinaryEntity } from '../binary-entity';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { MutationType } from './mutation-type';

const INPUT_UP_BIT = 1;
const INPUT_DOWN_BIT = INPUT_UP_BIT << 1;
const INPUT_LEFT_BIT = INPUT_DOWN_BIT << 1;
const INPUT_RIGHT_BIT = INPUT_LEFT_BIT << 1;
const INPUT_JUMP_BIT = INPUT_RIGHT_BIT << 1;

const EIGHTH = Math.PI / 4;

const ANGLE_MAP = [makeAngleMapRow(3, 2, 1), makeAngleMapRow(4, 0, 0), makeAngleMapRow(5, 6, 7)];

function makeAngleMapRow(xMinusOne: number, xZero: number, xOne: number) {
    return [xMinusOne, xZero, xOne].map((eighthParts) => eighthParts * EIGHTH);
}

export class PlayerInputMutation extends BinaryEntity {
    constructor(
        readonly id: number,
        readonly frameId: number,
        readonly angle: number,
        readonly up: boolean,
        readonly down: boolean,
        readonly left: boolean,
        readonly right: boolean,
        readonly wantsToJump: boolean
    ) {
        super();
    }

    private static applyInputToTargetMotion(
        targetMotion: { x: number; y: number },
        up: boolean,
        down: boolean,
        left: boolean,
        right: boolean,
        angle: number
    ) {
        const forwardComponent = (up ? 1 : 0) - (down ? 1 : 0);
        const rightComponent = (right ? 1 : 0) - (left ? 1 : 0);
        if (!forwardComponent && !rightComponent) {
            targetMotion.x = targetMotion.y = 0;
            return;
        }
        const motionAngle = ANGLE_MAP[1 + forwardComponent][1 + rightComponent] - angle;
        targetMotion.x = Math.cos(motionAngle);
        targetMotion.y = Math.sin(motionAngle);
    }

    static applyEncodedInputToTargetMotion(targetMotion: { x: number; y: number }, inputs: number, angle: number) {
        PlayerInputMutation.applyInputToTargetMotion(
            targetMotion,
            !!(inputs & INPUT_UP_BIT),
            !!(inputs & INPUT_DOWN_BIT),
            !!(inputs & INPUT_LEFT_BIT),
            !!(inputs & INPUT_RIGHT_BIT),
            angle
        );
    }

    static wantsToJump(inputs: number) {
        return !!(inputs & INPUT_JUMP_BIT);
    }

    applyToTargetMotion(targetMotion: { x: number; y: number }): void {
        PlayerInputMutation.applyInputToTargetMotion(
            targetMotion,
            this.up,
            this.down,
            this.left,
            this.right,
            this.angle
        );
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeByte(MutationType.UPDATE);
        writer.writeInt(this.id);
        writer.writeLong(this.frameId);
        writer.writeFloat(this.angle);
        writer.writeByte(
            (this.up ? INPUT_UP_BIT : 0) |
                (this.down ? INPUT_DOWN_BIT : 0) |
                (this.left ? INPUT_LEFT_BIT : 0) |
                (this.right ? INPUT_RIGHT_BIT : 0) |
                (this.wantsToJump ? INPUT_JUMP_BIT : 0)
        );
    }

    toString(): string {
        return [
            this.up && !this.down ? '⤊' : '',
            !this.up && this.down ? '⤋' : '',
            this.left && !this.right ? '⇚' : '',
            !this.left && this.right ? '⇛' : '',
            this.wantsToJump ? '↨' : '',
        ].join('');
    }
}
