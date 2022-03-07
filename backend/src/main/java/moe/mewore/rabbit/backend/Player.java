package moe.mewore.rabbit.backend;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import moe.mewore.rabbit.backend.net.Heart;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class Player extends BinaryEntity {

    @Getter
    private final int uid;

    @Getter
    private final int id;

    @Getter
    private final String username;

    @Getter
    private final boolean isReisen;

    @Getter
    private final PlayerState state = new PlayerState();

    @Getter
    @Setter
    private int latency = Heart.DEFAULT_LATENCY;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(id);
        output.writeAsciiWithLength(username);
        output.writeBoolean(isReisen);
        output.writeInt(latency);
        state.appendToBinaryOutput(output);
    }
}
