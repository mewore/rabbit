package moe.mewore.rabbit.backend.simulation;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

import moe.mewore.rabbit.backend.simulation.data.FrameSerializableEntity;
import moe.mewore.rabbit.backend.simulation.player.Player;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;

public interface World<I extends PlayerInput, P extends Player<I>> extends FrameSerializableEntity {

    boolean hasPlayers();

    int getMaxPlayerCount();

    long getFrameId();

    int getFrameSize();

    Map<Integer, P> getPlayersAsMap();

    /**
     * Execute something for every player currently in this world. This may run in parallel, depending on the
     * implementation! If you want to iterate through the players sequentially, use {@link World#getPlayersAsMap()}
     *
     * @param playerConsumer The code to call for every existing player.
     */
    void forEachPlayer(final Consumer<P> playerConsumer);

    boolean removePlayer(P player);

    void doStep(float deltaSeconds);

    void applyInputs(List<Queue<I>> pendingInputs, boolean force);
}
