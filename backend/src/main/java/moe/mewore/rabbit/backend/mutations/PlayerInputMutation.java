package moe.mewore.rabbit.backend.mutations;

import java.io.DataInput;
import java.io.IOException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.game.RabbitPlayerInput;

@RequiredArgsConstructor
public class PlayerInputMutation {

    @Getter
    private final RabbitPlayerInput input;

    public static PlayerInputMutation decodeFromBinary(final DataInput input) throws IOException {
        return new PlayerInputMutation(RabbitPlayerInput.decodeFromBinary(input));
    }
}
