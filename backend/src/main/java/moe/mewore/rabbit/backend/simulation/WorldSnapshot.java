package moe.mewore.rabbit.backend.simulation;

import lombok.Getter;
import lombok.Setter;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;

@Setter
public class WorldSnapshot {

    @Getter
    private final double[] doubleData;

    @Getter
    private final float[] floatData;

    @Getter
    private final int[] intData;

    private final int floatDataPerPlayer;

    private final int intDataPerPlayer;

    public WorldSnapshot(final int maxPlayerCount, final int intDataPerPlayer, final int doubleDataPerPlayer,
        final int floatDataPerPlayer) {
        this.intDataPerPlayer = intDataPerPlayer;
        this.floatDataPerPlayer = floatDataPerPlayer;
        intData = new int[maxPlayerCount * intDataPerPlayer];
        doubleData = new double[maxPlayerCount * doubleDataPerPlayer];
        floatData = new float[maxPlayerCount * floatDataPerPlayer];
    }

    public void registerInput(final Player player, final PlayerInputMutation input) {
        final int intIndex = player.getId() * intDataPerPlayer;
        final int oldPlayerUid = intData[intIndex + WorldState.PLAYER_UID_OFFSET];
        if (player.getUid() > oldPlayerUid || input.getId() >= intData[intIndex + WorldState.PLAYER_INPUT_ID_OFFSET]) {
            intData[intIndex + WorldState.PLAYER_INPUT_ID_OFFSET] = input.getId();
            intData[intIndex + WorldState.PLAYER_INPUT_KEYS_OFFSET] = input.getKeys();
            floatData[player.getId() * floatDataPerPlayer + WorldState.PLAYER_INPUT_ANGLE_OFFSET] = input.getAngle();
        } else {
            System.out.printf(
                "Not storing input #%d for player with UID %d; the old player UID is %d and the old input ID is #%d%n",
                input.getId(), player.getUid(), oldPlayerUid, intData[intIndex + WorldState.PLAYER_INPUT_ID_OFFSET]);
        }
    }

    public void copy(final WorldSnapshot previous) {
        System.arraycopy(previous.intData, 0, intData, 0, intData.length);
        System.arraycopy(previous.doubleData, 0, doubleData, 0, doubleData.length);
        System.arraycopy(previous.floatData, 0, floatData, 0, floatData.length);
    }
}
