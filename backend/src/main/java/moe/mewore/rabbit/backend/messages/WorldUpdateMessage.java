package moe.mewore.rabbit.backend.messages;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.simulation.WorldState;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class WorldUpdateMessage extends BinaryEntity {

    private final WorldState state;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.UPDATE.getIndex());
        state.appendToBinaryOutput(output);
    }
}
