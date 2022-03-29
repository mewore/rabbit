package moe.mewore.rabbit.backend.simulation.data;

public interface FrameSerializableEntity {

    void load(byte[] frame);

    void store(byte[] frame);
}
