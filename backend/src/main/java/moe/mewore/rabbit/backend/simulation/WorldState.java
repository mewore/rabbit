package moe.mewore.rabbit.backend.simulation;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class WorldState extends BinaryEntity {

    // float(input angle) + Vector3 + Vector3 + Vector2
    private static final int DOUBLE_DATA_PER_PLAYER = 3 + 3 + 2 + 1;

    // int(uid), int(input ID) + [5 x boolean](input directions, wants to jump)
    private static final int INT_DATA_PER_PLAYER = 3;

    public static final int BYTES_PER_STORED_STATE = DOUBLE_DATA_PER_PLAYER * 8 + INT_DATA_PER_PLAYER * 4 + 8;

    private static final long PARALLELISM_THRESHOLD = 5L;

    private final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

    @Getter
    private final int maxPlayerCount;

    private int playerUid = 0;

    private int timestamp = 0;

    public boolean hasPlayers() {
        return !players.isEmpty();
    }

    public Map<Integer, Player> getPlayers() {
        return Collections.unmodifiableMap(players);
    }

    public @Nullable Player createPlayer(final boolean isReisen) {
        final AtomicReference<Player> result = new AtomicReference<>();
        final Function<Integer, Player> mappingFunction = (key) -> {
            result.set(new Player(++playerUid, key, "Player " + (key + 1), isReisen));
            return result.get();
        };
        for (int i = 0; i < maxPlayerCount; i++) {
            players.computeIfAbsent(i, mappingFunction);
            if (result.get() != null) {
                return result.get();
            }
        }
        return null;
    }

    /**
     * @param player The player to remove
     * @return Whether the player was in this world in the first place.
     */
    public boolean removePlayer(final Player player) {
        return players.remove(player.getId(), player);
    }

    WorldSnapshot createEmptySnapshot() {
        return new WorldSnapshot(maxPlayerCount, DOUBLE_DATA_PER_PLAYER, INT_DATA_PER_PLAYER);
    }

    void simulate(final double dt, final int dtMilliseconds) {
        // TODO: Wrap player positions?
        players.forEachValue(PARALLELISM_THRESHOLD, player -> player.getState().applyTime(dt));
        timestamp += dtMilliseconds;
    }

    void load(final WorldSnapshot snapshot) {
        load(snapshot.getPlayerIntData(), snapshot.getPlayerDoubleData());
        timestamp = snapshot.getTimestamp();
    }

    // Assigning new indices helps avoid bugs when rearranging/adding/removing lines
    @SuppressWarnings("UnusedAssignment")
    private void load(final int[] intData, final double[] doubleData) {
        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            int intIndex = player.getId() * INT_DATA_PER_PLAYER;
            final int uid = intData[intIndex++];
            if (player.getUid() != uid) {
                return;
            }

            int doubleIndex = player.getId() * DOUBLE_DATA_PER_PLAYER;
            player.getState().applyInput(intData[intIndex++], doubleData[doubleIndex++], intData[intIndex++]);

            doubleIndex = player.getState().getPosition().load(doubleData, doubleIndex);
            doubleIndex = player.getState().getMotion().load(doubleData, doubleIndex);
        });
    }

    void store(final WorldSnapshot snapshot) {
        store(snapshot.getPlayerIntData(), snapshot.getPlayerDoubleData());
        snapshot.setTimestamp(timestamp);
    }

    // Assigning new indices ensures that bugs can be avoided when rearranging/adding/removing lines
    @SuppressWarnings("UnusedAssignment")
    private void store(final int[] intData, final double[] doubleData) {
        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            int intIndex = player.getId() * INT_DATA_PER_PLAYER;
            int doubleIndex = player.getId() * DOUBLE_DATA_PER_PLAYER;
            final boolean playerIsNew = player.getUid() > intData[intIndex];
            intData[intIndex++] = player.getUid();
            if (playerIsNew || player.getState().getInputId() >= intData[intIndex]) {
                intData[intIndex++] = player.getState().getInputId();
                intData[intIndex++] = player.getState().getInputKeys();
                doubleData[doubleIndex++] = player.getState().getInputAngle();
            } else {
                intIndex += 2;
                ++doubleIndex;
            }

            doubleIndex = player.getState().getPosition().store(doubleData, doubleIndex);
            doubleIndex = player.getState().getMotion().store(doubleData, doubleIndex);
        });
    }

    @Synchronized
    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(players.size());
        players.forEachEntry(Long.MAX_VALUE, entry -> {
            output.writeInt(entry.getKey());
            output.writeInt(entry.getValue().getLatency());
            output.writeInt(entry.getValue().getState().getInputId());
            entry.getValue().getState().getPosition().appendToBinaryOutput(output);
            entry.getValue().getState().getMotion().appendToBinaryOutput(output);
        });
    }
}
