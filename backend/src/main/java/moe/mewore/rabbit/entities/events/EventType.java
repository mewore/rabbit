package moe.mewore.rabbit.entities.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EventType {
    CONNECT(0),
    UPDATE(1),
    DISCONNECT(2);

    private final int index;
}
