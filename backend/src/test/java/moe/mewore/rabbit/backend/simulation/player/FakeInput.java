package moe.mewore.rabbit.backend.simulation.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class FakeInput implements PlayerInput {

    private final int id;

    @Setter
    private long frameId;
}
