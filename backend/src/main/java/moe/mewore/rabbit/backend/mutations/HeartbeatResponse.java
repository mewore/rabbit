package moe.mewore.rabbit.backend.mutations;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HeartbeatResponse {

    @Getter
    private final int id;

    public static HeartbeatResponse decodeFromBinary(final DataInput input) throws IOException {
        return new HeartbeatResponse(input.readInt());
    }
}
