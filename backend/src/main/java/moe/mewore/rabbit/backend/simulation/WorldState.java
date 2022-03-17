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
import moe.mewore.rabbit.world.MazeMap;

@RequiredArgsConstructor
public class WorldState extends BinaryEntity {

    // int(uid), int(input ID) + [5 x boolean](input directions, wants to jump)
    private static int INT_DATA_PER_PLAYER = 0;

    static final int PLAYER_UID_OFFSET = INT_DATA_PER_PLAYER++;

    static final int PLAYER_INPUT_ID_OFFSET = INT_DATA_PER_PLAYER++;

    static final int PLAYER_INPUT_KEYS_OFFSET = INT_DATA_PER_PLAYER++;

    // Vector3 + Vector3
    private static int DOUBLE_DATA_PER_PLAYER = 0;

    static final int PLAYER_POSITION_OFFSET = makeDoubleField(3);

    static final int PLAYER_MOTION_OFFSET = makeDoubleField(3);

    // [input angle]
    private static int FLOAT_DATA_PER_PLAYER = 0;

    static final int PLAYER_INPUT_ANGLE_OFFSET = FLOAT_DATA_PER_PLAYER++;

    public static final int BYTES_PER_STORED_STATE =
        DOUBLE_DATA_PER_PLAYER * 8 + (INT_DATA_PER_PLAYER + FLOAT_DATA_PER_PLAYER) * 4 + 3 * 8 + 8;

    private static final long PARALLELISM_THRESHOLD = 5L;

    private final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

    @Getter
    private final int maxPlayerCount;

    private final MazeMap map;

    @Getter
    private int frameId = 0;

    private int playerUid = 0;

    @SuppressWarnings("SameParameterValue")
    private static int makeDoubleField(final int size) {
        final int result = DOUBLE_DATA_PER_PLAYER;
        DOUBLE_DATA_PER_PLAYER += size;
        return result;
    }

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
        return new WorldSnapshot(maxPlayerCount, INT_DATA_PER_PLAYER, DOUBLE_DATA_PER_PLAYER, FLOAT_DATA_PER_PLAYER);
    }

    void doStep(final double deltaSeconds) {
        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            player.getState().applyTime(deltaSeconds);
            map.wrapPosition(player.getState().getPosition());
        });
        ++frameId;
    }

    void load(final WorldSnapshot snapshot, final int snapshotFrameId) {
        frameId = snapshotFrameId;

        final int[] intData = snapshot.getIntData();
        final double[] doubleData = snapshot.getDoubleData();
        final float[] floatData = snapshot.getFloatData();

        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            final int intIndex = player.getId() * INT_DATA_PER_PLAYER;
            final int uid = intData[intIndex + PLAYER_UID_OFFSET];
            if (player.getUid() != uid) {
                return;
            }

            final int floatIndex = player.getId() * FLOAT_DATA_PER_PLAYER;
            player.getState()
                .applyInput(intData[intIndex + PLAYER_INPUT_ID_OFFSET],
                    floatData[floatIndex + PLAYER_INPUT_ANGLE_OFFSET], intData[intIndex + PLAYER_INPUT_KEYS_OFFSET]);

            final int doubleIndex = player.getId() * DOUBLE_DATA_PER_PLAYER;
            player.getState().getPosition().load(doubleData, doubleIndex + PLAYER_POSITION_OFFSET);
            player.getState().getMotion().load(doubleData, doubleIndex + PLAYER_MOTION_OFFSET);
        });
    }

    void loadInput(final WorldSnapshot snapshot) {
        final int[] intData = snapshot.getIntData();
        final float[] floatData = snapshot.getFloatData();

        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            final int intIndex = player.getId() * INT_DATA_PER_PLAYER;
            if (player.getUid() != intData[intIndex + PLAYER_UID_OFFSET]) {
                return;
            }

            final int floatIndex = player.getId() * FLOAT_DATA_PER_PLAYER;
            player.getState()
                .applyInput(intData[intIndex + PLAYER_INPUT_ID_OFFSET],
                    floatData[floatIndex + PLAYER_INPUT_ANGLE_OFFSET], intData[intIndex + PLAYER_INPUT_KEYS_OFFSET]);
        });
    }

    void store(final WorldSnapshot snapshot) {
        final int[] intData = snapshot.getIntData();
        final double[] doubleData = snapshot.getDoubleData();
        final float[] floatData = snapshot.getFloatData();

        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            final int intIndex = player.getId() * INT_DATA_PER_PLAYER;
            final int doubleIndex = player.getId() * DOUBLE_DATA_PER_PLAYER;
            final int floatIndex = player.getId() * FLOAT_DATA_PER_PLAYER;
            final boolean playerIsNew = player.getUid() > intData[intIndex + PLAYER_UID_OFFSET];
            intData[intIndex + PLAYER_UID_OFFSET] = player.getUid();
            if (playerIsNew || player.getState().getInputId() >= intData[intIndex + PLAYER_INPUT_ID_OFFSET]) {
                intData[intIndex + PLAYER_INPUT_ID_OFFSET] = player.getState().getInputId();
                intData[intIndex + PLAYER_INPUT_KEYS_OFFSET] = player.getState().getInputKeys();
                floatData[floatIndex + PLAYER_INPUT_ANGLE_OFFSET] = player.getState().getInputAngle();
            }

            player.getState().getPosition().store(doubleData, doubleIndex + PLAYER_POSITION_OFFSET);
            player.getState().getMotion().store(doubleData, doubleIndex + PLAYER_MOTION_OFFSET);
        });
    }

    @Synchronized
    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(frameId);
        output.writeInt(players.size());
        players.forEachEntry(Long.MAX_VALUE, entry -> {
            output.writeInt(entry.getKey());
            output.writeInt(entry.getValue().getLatency());
            output.writeInt(entry.getValue().getState().getInputId());
            output.writeByte(entry.getValue().getState().getInputKeys());
            output.writeFloat(entry.getValue().getState().getInputAngle());
            entry.getValue().getState().getPosition().appendToBinaryOutput(output);
            entry.getValue().getState().getMotion().appendToBinaryOutput(output);
        });
    }
}
