package moe.mewore.rabbit.backend.net;

import java.util.function.BiConsumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Synchronized;
import moe.mewore.rabbit.backend.simulation.player.RabbitPlayer;

public class MultiPlayerHeart {

    private static final int MILLISECONDS_PER_BEAT = 1000 / 3;

    private final int maximumNumberOfPlayers;

    private final @Nullable Heart[] hearts;

    private final BiConsumer<@NonNull Integer, @NonNull Integer> sendFunction;

    private int currentTargetId = 0;

    public MultiPlayerHeart(final int maximumNumberOfPlayers,
        final BiConsumer<@NonNull Integer, @NonNull Integer> sendFunction) {
        this.maximumNumberOfPlayers = maximumNumberOfPlayers;
        this.sendFunction = sendFunction;
        hearts = new Heart[maximumNumberOfPlayers];
    }

    @Synchronized
    public void doStep() {
        final @Nullable Heart heart = hearts[currentTargetId];
        if (heart != null) {
            final Integer beatId = heart.prepareBeat();
            if (beatId != null) {
                sendFunction.accept(currentTargetId, beatId);
            }
        }
        currentTargetId = ++currentTargetId % maximumNumberOfPlayers;
    }

    public long getStepTimeInterval() {
        return MILLISECONDS_PER_BEAT / maximumNumberOfPlayers;
    }

    public void addPlayer(final RabbitPlayer player) {
        hearts[player.getId()] = new Heart(player::setLatency);
    }

    public void removePlayer(final RabbitPlayer player) {
        hearts[player.getId()] = null;
    }

    @Synchronized
    public void receive(final RabbitPlayer player, final int heartbeatId) {
        final @Nullable Heart heart = hearts[player.getId()];
        if (heart != null) {
            heart.receive(heartbeatId);
        }
    }
}
