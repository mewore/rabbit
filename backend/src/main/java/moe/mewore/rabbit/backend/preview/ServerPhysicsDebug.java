package moe.mewore.rabbit.backend.preview;

import javax.vecmath.Vector3f;
import java.awt.*;

import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.IDebugDraw;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ServerPhysicsDebug extends IDebugDraw {

    private static final int MAX_LINE_COUNT = 10000;

    private static final int HASH_MULTIPLIER = 31;

    private final int[] coordinates = new int[MAX_LINE_COUNT * 4];

    private final Color[] colors = new Color[MAX_LINE_COUNT];

    private final float mapWidth;

    private final float mapDepth;

    private final int imageWidth;

    private final int imageHeight;

    @Getter
    @Setter
    private int debugMode =
        DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_AABB | DebugDrawModes.DRAW_CONTACT_POINTS;

    @Getter
    private int hash = 1;

    @Getter
    private int lineCount = 0;

    public void draw(final LineDrawer drawer) {
        for (int i = 0; i < lineCount; i++) {
            drawer.drawLine(coordinates[i << 2], coordinates[(i << 2) + 1], coordinates[(i << 2) + 2],
                coordinates[(i << 2) + 3], colors[i]);
        }
    }

    public void clear() {
        lineCount = 0;
        hash = 1;
    }

    @Override
    public void drawLine(final Vector3f from, final Vector3f to, final Vector3f color) {
        if (lineCount >= MAX_LINE_COUNT) {
            return;
        }
        final int coordinateIndex = lineCount * 4;

        final int x1 = Math.round((from.x / mapWidth + .5f) * imageWidth);
        final int y1 = Math.round((from.z / mapDepth + .5f) * imageHeight);
        final int x2 = Math.round((to.x / mapWidth + .5f) * imageWidth);
        final int y2 = Math.round((to.z / mapDepth + .5f) * imageHeight);
        if (x1 == x2 && y1 == y2) {
            return;
        }

        final Color graphicsColor = new Color(Math.min(color.x, 1f), Math.min(color.y, 1f), Math.min(color.z, 1f));

        hash = HASH_MULTIPLIER * hash + x1;
        hash = HASH_MULTIPLIER * hash + x2;
        hash = HASH_MULTIPLIER * hash + y1;
        hash = HASH_MULTIPLIER * hash + y2;
        hash = HASH_MULTIPLIER * hash + graphicsColor.getRGB();

        coordinates[coordinateIndex] = x1;
        coordinates[coordinateIndex + 1] = y1;
        coordinates[coordinateIndex + 2] = x2;
        coordinates[coordinateIndex + 3] = y2;
        colors[lineCount] = graphicsColor;
        ++lineCount;
    }

    @Override
    public void drawContactPoint(final Vector3f pointOnB, final Vector3f normalOnB, final float distance,
        final int lifeTime, final Vector3f color) {
        final Vector3f targetPoint = new Vector3f(normalOnB);
        targetPoint.scale(distance);
        targetPoint.add(pointOnB);
        drawLine(pointOnB, targetPoint, color);
    }

    @Override
    public void reportErrorWarning(final String warningString) {
        System.err.println("Error/warning: " + warningString);
    }

    @Override
    public void draw3dText(final Vector3f location, final String textString) {
        final int x = Math.round((location.x / mapWidth + .5f) * imageWidth);
        final int y = Math.round((location.z / mapDepth + .5f) * imageHeight);
        System.err.printf("3D text at (%d, %d): %s%n", x, y, textString);
    }
}
