package moe.mewore.rabbit.backend.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class HeartbeatRequest extends BinaryEntity {

    private final int id;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.HEARTBEAT_REQUEST.getIndex());
        output.writeInt(id);
    }
}
