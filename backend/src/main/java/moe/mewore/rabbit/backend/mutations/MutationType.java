package moe.mewore.rabbit.backend.mutations;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MutationType {
    PLAYER_JOIN((byte) 0),
    PLAYER_INPUT((byte) 1);

    private final byte index;
}
