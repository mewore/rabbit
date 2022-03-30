package moe.mewore.rabbit.backend.simulation.player;

public class FakePlayerInputEvent extends PlayerInputEvent {

    public FakePlayerInputEvent(final RabbitPlayer player, final PlayerInput input) {
        super(player.getId(), player.getUid(), input);
    }
}
