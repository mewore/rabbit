package moe.mewore.rabbit.backend.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MessageType {
    JOIN((byte) 0),
    MAP_DATA((byte) 1),
    UPDATE((byte) 2),
    DISCONNECT((byte) 3),
    HEARTBEAT_REQUEST((byte) 4);

    private final byte index;
}
