import { BinaryEntity } from './binary-entity';
import { SignedBinaryReader } from './data/signed-binary-reader';
import { SignedBinaryWriter } from './data/signed-binary-writer';
import { Vector2Entity } from './vector2-entity';
import { Vector3Entity } from './vector3-entity';

export class PlayerState extends BinaryEntity {
    constructor(
        readonly position: Vector3Entity,
        readonly motion: Vector3Entity,
        readonly targetMotion: Vector2Entity
    ) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.position.appendToBinaryOutput(writer);
        this.motion.appendToBinaryOutput(writer);
        this.targetMotion.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerState {
        return new PlayerState(
            Vector3Entity.decodeFromBinary(reader),
            Vector3Entity.decodeFromBinary(reader),
            Vector2Entity.decodeFromBinary(reader)
        );
    }
}
