package moe.mewore.rabbit.entities.mutations;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MutationType {
    SET_UP(0),
    UPDATE(1);

    private final int index;
}
