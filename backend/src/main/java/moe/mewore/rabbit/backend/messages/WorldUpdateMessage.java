package moe.mewore.rabbit.backend.messages;

import java.util.Collection;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.simulation.WorldSnapshot;
import moe.mewore.rabbit.backend.simulation.WorldState;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class WorldUpdateMessage extends BinaryEntity {

    private final WorldState worldState;

    private final WorldSnapshot snapshot;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.UPDATE.getIndex());

        final Collection<Player> players = worldState.getPlayers().values();
        output.writeInt(players.size());
        for (final Player player : players) {
            output.writeInt(player.getId());
            output.writeInt(player.getLatency());
        }

        output.writeInt(worldState.getMaxPlayerCount());
        output.writeInt(worldState.getSpheres().length);
        snapshot.appendToBinaryOutput(output);
    }
}
