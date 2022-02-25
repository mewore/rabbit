package moe.mewore.rabbit.backend.mutations;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MutationType {
    JOIN((byte) 0),
    UPDATE((byte) 1);

    private final byte index;
}
