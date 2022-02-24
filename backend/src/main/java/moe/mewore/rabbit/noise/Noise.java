package moe.mewore.rabbit.noise;

import java.awt.image.BufferedImage;

public interface Noise {

    double get(final double x, final double y);

    BufferedImage render(final int width, final int height);

    void render(final int width, final int height, final String name);
}
