package moe.mewore.rabbit.backend.simulation.player;

import moe.mewore.rabbit.backend.simulation.data.FrameSerializableEntity;

public interface Player<I extends PlayerInput> extends FrameSerializableEntity {

    int getInputId();

    void applyInput(I input);

    int getUid();

    int getIndex();
}
