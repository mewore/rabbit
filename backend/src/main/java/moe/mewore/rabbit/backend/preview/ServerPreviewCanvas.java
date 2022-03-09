package moe.mewore.rabbit.backend.preview;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.simulation.WorldState;
import moe.mewore.rabbit.world.MazeMap;

public class ServerPreviewCanvas extends Canvas {

    private static final int PLAYER_CIRCLE_RADIUS = 15;

    private static final int PIXELS_PER_CELL = 40;

    private static final int MAX_IMAGE_PIXELS = 10000000;

    private static final BasicStroke stroke = new BasicStroke((float) (PLAYER_CIRCLE_RADIUS) / 2f);

    private static final int FONT_SIZE = 16;

    private static final int FONT_BASELINE = 2;

    private static final int BOTTOM_TEXT_OFFSET_Y = -(PLAYER_CIRCLE_RADIUS * 2 - FONT_BASELINE);

    private static final Font FONT = new Font(null, Font.BOLD, FONT_SIZE);

    private static final Color PLAYER_CIRCLE_INNER_COLOR = new Color(255, 255, 255, 200);

    private static final Color PLAYER_CIRCLE_OUTER_COLOR = new Color(0, 25, 50, 255);

    private static final Color PLAYER_TEXT_COLOR = PLAYER_CIRCLE_OUTER_COLOR;

    private static final int TEXT_BACKGROUND_PADDING = 2;

    private static final Color PLAYER_TEXT_BACKGROUND_COLOR = new Color(255, 255, 255, 150);

    private static final Color MOTION_COLOR = new Color(50, 150, 20, 200);

    private static final Color TARGET_MOTION_COLOR = new Color(255, 100, 20, 150);

    private final MazeMap map;

    private final WorldState worldState;

    private final BufferedImage mapImage;

    private final BufferedImage image;

    private int lastPreviewHash = -1;

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

    public void updatePlayerOverlay() {
        final List<PlayerPreview> playerPreviews = makeCurrentPlayerPreviews();
        final int newPreviewHash = playerPreviews.hashCode();
        if (newPreviewHash == lastPreviewHash) {
            // Assuming it's exactly the same so there's no need to redraw it
            return;
        }
        lastPreviewHash = newPreviewHash;

        cloneImage(mapImage, image);
        drawPlayerPreviews(playerPreviews);
        paint(getGraphics());
    }

    private void drawPlayerPreviews(final List<PlayerPreview> playerPreviews) {
        if (playerPreviews.isEmpty()) {
            return;
        }

        final Graphics2D graphics = image.createGraphics();
        graphics.setFont(FONT);
        graphics.setStroke(stroke);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        offsetX = offsetY = 0;

        for (final PlayerPreview preview : playerPreviews) {
            offsetX -= preview.getX() - image.getWidth() / 2;
            offsetY -= preview.getY() - image.getHeight() / 2;

            for (int dx = -image.getWidth(); dx <= image.getWidth(); dx += image.getWidth()) {
                for (int dy = -image.getHeight(); dy <= image.getHeight(); dy += image.getHeight()) {
                    final int x = preview.getX() + dx;
                    final int y = preview.getY() + dy;

                    graphics.setColor(PLAYER_CIRCLE_INNER_COLOR);
                    graphics.fillOval(x - PLAYER_CIRCLE_RADIUS, y - PLAYER_CIRCLE_RADIUS, PLAYER_CIRCLE_RADIUS * 2,
                        PLAYER_CIRCLE_RADIUS * 2);
                    graphics.setColor(PLAYER_CIRCLE_OUTER_COLOR);
                    graphics.drawOval(x - PLAYER_CIRCLE_RADIUS, y - PLAYER_CIRCLE_RADIUS, PLAYER_CIRCLE_RADIUS * 2,
                        PLAYER_CIRCLE_RADIUS * 2);
                    if (preview.hasTargetMotionLine()) {
                        graphics.setColor(TARGET_MOTION_COLOR);
                        graphics.drawLine(x, y, preview.getTargetMotionX() + dx, preview.getTargetMotionY() + dy);
                    }
                    if (preview.hasMotionLine()) {
                        graphics.setColor(MOTION_COLOR);
                        graphics.drawLine(x, y, preview.getMotionX() + dx, preview.getMotionY() + dy);
                    }
                }
            }
        }

        final FontMetrics fontMetrics = graphics.getFontMetrics(FONT);

        // The text should be above everything else, so it should be drawn last.
        for (final PlayerPreview preview : playerPreviews) {
            final int bottomTextWidth = fontMetrics.stringWidth(preview.getBottomText());
            final int topTextWidth = fontMetrics.stringWidth(preview.getTopText());

            for (int dx = -image.getWidth(); dx <= image.getWidth(); dx += image.getWidth()) {
                for (int dy = -image.getHeight(); dy <= image.getHeight(); dy += image.getHeight()) {
                    final int x = preview.getX() + dx;
                    final int y = preview.getY() + dy;

                    final int bottomTextX = x - bottomTextWidth / 2;
                    final int bottomTextY = y + BOTTOM_TEXT_OFFSET_Y;

                    final int topTextX = x - topTextWidth / 2;
                    final int topTextY = bottomTextY - FONT_SIZE;

                    graphics.setColor(PLAYER_TEXT_BACKGROUND_COLOR);
                    graphics.fillRoundRect(Math.min(topTextX, bottomTextX) - TEXT_BACKGROUND_PADDING,
                        Math.min(topTextY, bottomTextY) - FONT_SIZE + FONT_BASELINE - TEXT_BACKGROUND_PADDING,
                        Math.max(topTextWidth, bottomTextWidth) + TEXT_BACKGROUND_PADDING * 2,
                        FONT_SIZE * 2 + TEXT_BACKGROUND_PADDING * 2, TEXT_BACKGROUND_PADDING, TEXT_BACKGROUND_PADDING);

                    graphics.setColor(PLAYER_TEXT_COLOR);
                    graphics.drawString(preview.getTopText(), topTextX, topTextY);
                    graphics.drawString(preview.getBottomText(), bottomTextX, bottomTextY);
                }
            }
        }

        offsetX /= playerPreviews.size();
        offsetY /= playerPreviews.size();
    }

    private List<PlayerPreview> makeCurrentPlayerPreviews() {
        return worldState.getPlayers().values().stream().map(this::makePlayerPreview).collect(Collectors.toList());
    }

    private PlayerPreview makePlayerPreview(final Player player) {
        final int x = (int) (wrapNormalized(player.getState().getPosition().getX() / map.getWidth() + 0.5) *
            image.getWidth());
        final int y = (int) (wrapNormalized(player.getState().getPosition().getZ() / map.getDepth() + 0.5) *
            image.getHeight());

        final int motionX = x + (int) (player.getState().getMotion().getX() * image.getWidth() / map.getWidth());
        final int motionY = y + (int) (player.getState().getMotion().getZ() * image.getHeight() / map.getDepth());
        final int targetMotionX =
            x + (int) (player.getState().getTargetHorizontalMotion().getX() * image.getWidth() / map.getWidth());
        final int targetMotionY =
            y + (int) (player.getState().getTargetHorizontalMotion().getY() * image.getHeight() / map.getDepth());

        final int latency = player.getLatency();
        // In order to avoid too frequent updating of the label (and thus redrawing of the preview),
        final int latencyRounding = latency < 50 ? 5 : 10;
        final int roundedLatency = (int) Math.round((double) (latency) / latencyRounding) * latencyRounding;

        final String topText = player.getUsername();
        final String bottomText = String.format("(%d ms)", roundedLatency);

        return new PlayerPreview(x, y, motionX, motionY, targetMotionX, targetMotionY, topText, bottomText);
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
