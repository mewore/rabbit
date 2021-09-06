import { BinaryEntity } from '../binary-entity';
import { Player } from '../player';
import { SignedBinaryReader } from '../data/signed-binary-reader';
import { SignedBinaryWriter } from '../data/signed-binary-writer';

export class PlayerConnectEvent extends BinaryEntity {
    constructor(readonly player: Player) {
        super();
    }

    appendToBinaryOutput(writer: SignedBinaryWriter): void {
        this.player.appendToBinaryOutput(writer);
    }

    static decodeFromBinary(reader: SignedBinaryReader): PlayerConnectEvent {
        return new PlayerConnectEvent(Player.decodeFromBinary(reader));
    }
}
