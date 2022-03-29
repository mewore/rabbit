import { Input } from '@/game/util/input';

import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

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

export class PlayerInput extends BinaryEntity {
    public static EMPTY: PlayerInput = new PlayerInput(-1, -1, 0, 0);

    constructor(readonly id: number, readonly frameId: number, private readonly byte: number, readonly angle: number) {
        super();
    }

    static compressInput(input: Input): number {
        return (
            (input.isUpPressed ? INPUT_UP_BIT : 0) |
            (input.isDownPressed ? INPUT_DOWN_BIT : 0) |
            (input.isLeftPressed ? INPUT_LEFT_BIT : 0) |
            (input.isRightPressed ? INPUT_RIGHT_BIT : 0) |
            (input.wantsToJump ? INPUT_JUMP_BIT : 0)
        );
    }

    get wantsToJump(): boolean {
        return PlayerInput.wantsToJump(this.byte);
    }

    applyToTargetMotion(targetMotion: { x: number; y: number }): void {
        PlayerInput.applyEncodedInputToTargetMotion(targetMotion, this.byte, this.angle);
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeInt(this.id);
        writer.writeLong(this.frameId);
        writer.writeByte(this.byte);
        writer.writeFloat(this.angle);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerInput {
        return new PlayerInput(reader.readInt(), reader.readLong(), reader.readByte(), reader.readFloat());
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
        PlayerInput.applyInputToTargetMotion(
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

    toString(): string {
        const up = this.byte & INPUT_UP_BIT;
        const down = this.byte & INPUT_DOWN_BIT;
        const left = this.byte & INPUT_LEFT_BIT;
        const right = this.byte & INPUT_LEFT_BIT;
        return [
            up && !down ? '⤊' : '',
            !up && down ? '⤋' : '',
            left && !right ? '⇚' : '',
            !left && right ? '⇛' : '',
            this.wantsToJump ? '↨' : '',
        ].join('');
    }
}
