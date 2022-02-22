package moe.mewore.rabbit.noise;

public interface Noise {

    double get(final double x, final double y);

    void render(final int width, final int height, final String name);
}
