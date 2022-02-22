package moe.mewore.rabbit.noise;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class NoiseBase implements Noise {

    @Override
    public void render(final int width, final int height, final String name) {
        System.out.printf("Rendering noise into a %dx%d px image...%n", width, height);

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        final double xStep = 1.0 / width;
        final double yStep = 1.0 / height;
        double normalizedY = 0.0;
        double normalizedX;
        int value;
        for (int y = 0; y < height; y++, normalizedY += yStep) {
            normalizedX = 0.0;
            for (int x = 0; x < width; x++, normalizedX += xStep) {
                value = (int) (get(normalizedX, normalizedY) * 255.999);
                img.setRGB(x, y, new Color(value, value, value).getRGB());
            }
        }

        try {
            ImageIO.write(img, "png", new File(name + ".png"));
        } catch (final IOException e) {
            System.out.println("Error while creating maze preview: " + e);
        }
    }
}
