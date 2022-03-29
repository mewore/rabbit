package moe.mewore.rabbit.backend.simulation.player;

import moe.mewore.rabbit.backend.Player;

public class FakePlayerInputEvent extends PlayerInputEvent {

    public FakePlayerInputEvent(final Player player, final PlayerInput input) {
        super(player.getId(), player.getUid(), input);
    }
}
