package moe.mewore.rabbit.backend.simulation.player;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerInputEvent<I extends PlayerInput> {

    private final int playerId;

    private final int playerUid;

    private final I input;

    public long getFrameId() {
        return input.getFrameId();
    }

    public boolean canReplace(final @Nullable PlayerInputEvent<I> other) {
        return other == null || other.getPlayerUid() != playerUid || input.getId() > other.getInput().getId();
    }
}
