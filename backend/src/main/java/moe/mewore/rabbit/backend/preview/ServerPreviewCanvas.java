package moe.mewore.rabbit.backend.preview;

import java.awt.*;
import java.awt.image.BufferedImage;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.simulation.WorldState;
import moe.mewore.rabbit.world.MazeMap;

public class ServerPreviewCanvas extends Canvas {

    private static final int PLAYER_CIRCLE_RADIUS = 15;

    private static final int PIXELS_PER_CELL = 40;

    private static final int MAX_IMAGE_PIXELS = 10000000;

    private static final BasicStroke stroke = new BasicStroke((float) (PLAYER_CIRCLE_RADIUS) / 2f);

    private static final Font FONT = new Font(null, Font.BOLD, 16);

    private static final Color PLAYER_CIRCLE_INNER_COLOR = new Color(255, 255, 255, 200);

    private static final Color PLAYER_CIRCLE_OUTER_COLOR = new Color(0, 25, 50, 255);

    private static final Color MOTION_COLOR = new Color(50, 150, 20, 200);

    private static final Color TARGET_MOTION_COLOR = new Color(255, 100, 20, 150);

    private final MazeMap map;

    private final WorldState worldState;

    private final BufferedImage mapImage;

    private final BufferedImage image;

    private int offsetX = 0;

    private int offsetY = 0;

    public ServerPreviewCanvas(final MazeMap map, final WorldState initialWorldState) {
        super();
        int imageWidth = PIXELS_PER_CELL * map.getColumnCount();
        int imageHeight = PIXELS_PER_CELL * map.getRowCount();
        if (imageWidth * imageHeight > MAX_IMAGE_PIXELS) {
            final double overhead = (double) (imageWidth * imageHeight) / MAX_IMAGE_PIXELS;
            imageWidth = (int) (imageWidth / Math.sqrt(overhead));
            imageHeight = (int) (imageHeight / Math.sqrt(overhead));
        }
        this.map = map;
        worldState = initialWorldState;
        mapImage = map.render(imageWidth, imageHeight);
        image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
    }

    private static double wrapNormalized(final double coordinate) {
        return coordinate - Math.floor(coordinate);
    }

    /**
     * Assuming both images are with the same dimensions, make {@code target} exactly the same as {@code source}.
     *
     * @param source The source image.
     * @param target The target image.
     */
    private static void cloneImage(final BufferedImage source, final BufferedImage target) {
        final Graphics graphics = target.createGraphics();
        graphics.clearRect(0, 0, target.getWidth(), target.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
    }

    @SuppressWarnings("OverlyComplexMethod")
    public void updatePlayerOverlay() {
        cloneImage(mapImage, image);
        final Graphics2D graphics = image.createGraphics();
        graphics.setFont(FONT);
        graphics.setStroke(stroke);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        offsetX = offsetY = 0;
        int playerCount = 0;

        for (final Player player : worldState.getPlayers().values()) {
            final int x = (int) (wrapNormalized(player.getState().getPosition().getX() / map.getWidth() + 0.5) *
                image.getWidth());
            final int y = (int) (wrapNormalized(player.getState().getPosition().getZ() / map.getDepth() + 0.5) *
                image.getHeight());
            offsetX -= x - image.getWidth() / 2;
            offsetY -= y - image.getHeight() / 2;
            ++playerCount;

            final int motionX = x + (int) (player.getState().getMotion().getX() * image.getWidth() / map.getWidth());
            final int motionY = y + (int) (player.getState().getMotion().getZ() * image.getHeight() / map.getDepth());
            final int targetMotionX =
                x + (int) (player.getState().getTargetHorizontalMotion().getX() * image.getWidth() / map.getWidth());
            final int targetMotionY =
                y + (int) (player.getState().getTargetHorizontalMotion().getY() * image.getHeight() / map.getDepth());

            final String playerText = String.format("%s (%d ms)", player.getUsername(), player.getLatency());
            for (int dx = -image.getWidth(); dx <= image.getWidth(); dx += image.getWidth()) {
                for (int dy = -image.getHeight(); dy <= image.getHeight(); dy += image.getHeight()) {
                    graphics.setColor(PLAYER_CIRCLE_INNER_COLOR);
                    graphics.fillOval(x + dx - PLAYER_CIRCLE_RADIUS, y + dy - PLAYER_CIRCLE_RADIUS,
                        PLAYER_CIRCLE_RADIUS * 2, PLAYER_CIRCLE_RADIUS * 2);
                    graphics.setColor(PLAYER_CIRCLE_OUTER_COLOR);
                    graphics.drawOval(x + dx - PLAYER_CIRCLE_RADIUS, y + dy - PLAYER_CIRCLE_RADIUS,
                        PLAYER_CIRCLE_RADIUS * 2, PLAYER_CIRCLE_RADIUS * 2);
                    if ((targetMotionX != x || targetMotionY != y) &&
                        (targetMotionX != motionX || targetMotionY != motionY)) {
                        graphics.setColor(TARGET_MOTION_COLOR);
                        graphics.drawLine(x + dx, y + dy, targetMotionX + dx, targetMotionY + dy);
                    }
                    if (motionX != x || motionY != y) {
                        graphics.setColor(MOTION_COLOR);
                        graphics.drawLine(x + dx, y + dy, motionX + dx, motionY + dy);
                    }
                    graphics.setColor(PLAYER_CIRCLE_OUTER_COLOR);
                    graphics.drawString(playerText, x + dx + PLAYER_CIRCLE_RADIUS * 2, y);
                }
            }
        }
        if (playerCount > 0) {
            offsetX /= playerCount;
            offsetY /= playerCount;
        }

        paint(getGraphics());
    }

    @Override
    public void paint(final Graphics graphics) {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        final int realOffsetX = offsetX + (getWidth() - image.getWidth()) / 2;
        final int realOffsetY = offsetY + (getHeight() - image.getHeight()) / 2;
        final int minX = realOffsetX - (realOffsetX / image.getWidth() + 1) * image.getWidth();
        final int minY = realOffsetY - (realOffsetY / image.getHeight() + 1) * image.getHeight();
        for (int y = minY; y < getHeight(); y += image.getHeight()) {
            for (int x = minX; x < getWidth(); x += image.getWidth()) {
                graphics.drawImage(image, x, y, this);
            }
        }
    }

}
