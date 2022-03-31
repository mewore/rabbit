package moe.mewore.rabbit.backend.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.game.RabbitPlayerInput;
import moe.mewore.rabbit.backend.game.RabbitWorld;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class WorldUpdateMessage extends BinaryEntity {

    private final RabbitWorld world;

    private final @Nullable List<PlayerInputEvent<RabbitPlayerInput>> appliedInputs;

    private final byte[] frame;

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeByte(MessageType.UPDATE.getIndex());

        output.writeInt(world.getMaxPlayerCount());
        output.writeInt(world.getSpheres().length);

        output.writeMap(world.getPlayersAsMap(), output::writeInt, player -> output.writeInt(player.getLatency()));

        final Map<Integer, List<RabbitPlayerInput>> inputsByPlayerId = new HashMap<>();
        if (appliedInputs != null) {
            for (final PlayerInputEvent<RabbitPlayerInput> inputEvent : appliedInputs) {
                final List<RabbitPlayerInput> inputList = inputsByPlayerId.getOrDefault(inputEvent.getPlayerId(),
                    new ArrayList<>());
                inputList.add(inputEvent.getInput());
                inputsByPlayerId.putIfAbsent(inputEvent.getPlayerId(), inputList);
            }
        }
        output.writeMap(inputsByPlayerId, output::writeInt, output::writeCollection);

        output.write(frame);
    }
}
