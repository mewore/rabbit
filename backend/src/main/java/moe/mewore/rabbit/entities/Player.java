package moe.mewore.rabbit.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class Player extends BinaryEntity {

    @Getter
    private final int id;

    private final String username;

    private final boolean isReisen;

    @Setter
    private PlayerState state = new PlayerState();

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(id);
        output.writeAsciiWithLength(username);
        output.writeBoolean(isReisen);
        state.appendToBinaryOutput(output);
    }
}
