package moe.mewore.rabbit.backend.simulation;

import java.util.function.Consumer;

import lombok.Getter;
import moe.mewore.rabbit.backend.simulation.player.Player;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;

public class FakeWorld extends WorldBase<PlayerInput, Player<PlayerInput>> {

    @Getter
    private float simulatedTime = 0f;

    public FakeWorld() {
        super(1);
    }

    @Override
    public void forEachPlayer(final Consumer<Player<PlayerInput>> playerConsumer) {

    }

    @Override
    public void doStep(final float deltaSeconds) {
        ++frameId;
        simulatedTime += deltaSeconds;
    }
}
