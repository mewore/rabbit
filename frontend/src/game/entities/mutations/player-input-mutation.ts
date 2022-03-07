import { BinaryEntity } from '../binary-entity';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { MutationType } from './mutation-type';

const INPUT_UP_BIT = 1;
const INPUT_DOWN_BIT = INPUT_UP_BIT << 1;
const INPUT_LEFT_BIT = INPUT_DOWN_BIT << 1;
const INPUT_RIGHT_BIT = INPUT_LEFT_BIT << 1;

export class PlayerInputMutation extends BinaryEntity {
    constructor(
        private readonly id: number,
        private readonly angle: number,
        private readonly up: boolean,
        private readonly down: boolean,
        private readonly left: boolean,
        private readonly right: boolean
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        writer.writeByte(MutationType.UPDATE);
        writer.writeInt(this.id);
        writer.writeFloat(this.angle);
        writer.writeByte(
            (this.up ? INPUT_UP_BIT : 0) |
                (this.down ? INPUT_DOWN_BIT : 0) |
                (this.left ? INPUT_LEFT_BIT : 0) |
                (this.right ? INPUT_RIGHT_BIT : 0)
        );
    }
}
