package moe.mewore.rabbit.backend.simulation.player;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class PlayerInputEvent {

    private final int playerId;

    private final int playerUid;

    @Setter
    private PlayerInput input;

    public long getFrameId() {
        return input.getFrameId();
    }

    public boolean canReplace(final @Nullable PlayerInputEvent other) {
        return other == null || other.getPlayerUid() != playerUid || input.getId() > other.getInput().getId();
    }
}
