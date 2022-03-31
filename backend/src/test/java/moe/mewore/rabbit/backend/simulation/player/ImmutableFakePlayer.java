package moe.mewore.rabbit.backend.simulation.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ImmutableFakePlayer implements Player<PlayerInput> {

    @Builder.Default
    private final int index = 0;

    private final int uid = 0;

    private final int inputId = -1;

    @Override
    public void load(final byte[] frame) {
    }

    @Override
    public void store(final byte[] frame) {
    }

    @Override
    public void applyInput(final PlayerInput input) {
    }

    @Override
    public int getUid() {
        return uid;
    }

    @Override
    public int getIndex() {
        return index;
    }
}
