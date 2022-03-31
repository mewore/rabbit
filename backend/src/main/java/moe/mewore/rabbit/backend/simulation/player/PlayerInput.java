package moe.mewore.rabbit.backend.simulation.player;

public interface PlayerInput {

    int getId();

    long getFrameId();

    void setFrameId(long frameId);
}
