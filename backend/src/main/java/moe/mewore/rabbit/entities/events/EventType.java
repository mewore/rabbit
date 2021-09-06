package moe.mewore.rabbit.entities.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EventType {
    CONNECT(0),
    SET_UP(1),
    UPDATE(2),
    DISCONNECT(3);

    private final int index;
}
