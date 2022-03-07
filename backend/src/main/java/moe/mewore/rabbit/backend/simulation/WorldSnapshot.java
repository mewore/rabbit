package moe.mewore.rabbit.backend.simulation;

import lombok.Getter;
import lombok.Setter;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;

@Setter
public class WorldSnapshot {

    @Getter
    private final double[] playerDoubleData;

    @Getter
    private final int[] playerIntData;

    private final int doubleDataPerPlayer;

    private final int intDataPerPlayer;

    @Setter
    @Getter
    private int timestamp = 0;

    public WorldSnapshot(final int maxPlayerCount, final int doubleDataPerPlayer, final int intDataPerPlayer) {
        this.intDataPerPlayer = intDataPerPlayer;
        this.doubleDataPerPlayer = doubleDataPerPlayer;
        playerDoubleData = new double[maxPlayerCount * doubleDataPerPlayer];
        playerIntData = new int[maxPlayerCount * intDataPerPlayer];
    }

    public void registerInput(final Player player, final PlayerInputMutation input) {
        final int index = player.getId() * intDataPerPlayer;
        if (playerIntData[index] < player.getUid() || input.getId() >= playerIntData[index + 1]) {
            playerIntData[index + 1] = input.getId();
            playerIntData[index + 2] = input.getKeys();
            playerDoubleData[player.getId() * doubleDataPerPlayer] = input.getAngle();
        }
    }

    public void copy(final WorldSnapshot previous) {
        System.arraycopy(previous.playerDoubleData, 0, playerDoubleData, 0, playerDoubleData.length);
        System.arraycopy(previous.playerIntData, 0, playerIntData, 0, playerIntData.length);
        timestamp = previous.timestamp;
    }
}
