package moe.mewore.rabbit.entities.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EventType {
    JOIN((byte) 0),
    UPDATE((byte) 2),
    DISCONNECT((byte) 3);

    private final byte index;
}
