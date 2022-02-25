package moe.mewore.rabbit.world.editor;

import java.awt.image.BufferedImage;

import moe.mewore.rabbit.noise.Noise;

public class FakeNoise implements Noise {

    @Override
    public double get(final double x, final double y) {
        return 0.0;
    }

    @Override
    public BufferedImage render(final int width, final int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void render(final int width, final int height, final String name) {
    }
}
