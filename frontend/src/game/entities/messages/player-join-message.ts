import { BinaryEntity } from '../binary-entity';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';
import { Player } from '../player';

export class PlayerJoinMessage extends BinaryEntity {
    constructor(readonly player: Player) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.player.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerJoinMessage {
        return new PlayerJoinMessage(Player.decodeFromBinary(reader));
    }
}
