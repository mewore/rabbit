package moe.mewore.rabbit.backend.preview;

import javax.vecmath.Vector3f;
import java.awt.*;

import com.bulletphysics.linearmath.DebugDrawModes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServerPhysicsDebugTest {

    private ServerPhysicsDebug debugDraw;

    @BeforeEach
    void setUp() {
        debugDraw = new ServerPhysicsDebug(1f, 1f, 1, 1);
    }

    @Test
    void testDrawLine() {
        assertEquals(1, debugDraw.getHash());
        debugDraw.drawLine(new Vector3f(), new Vector3f(1f, 1f, 1f), new Vector3f(.5f, .8f, .1f));
        assertNotEquals(1, debugDraw.getHash());
        assertEquals(1, debugDraw.getLineCount());
    }

    @Test
    void testDrawLine_sameCoordinates() {
        debugDraw.drawLine(new Vector3f(), new Vector3f(), new Vector3f(.5f, .8f, .1f));
        assertEquals(1, debugDraw.getHash());
        assertEquals(0, debugDraw.getLineCount());
    }

    @Test
    void testDraw() {
        debugDraw.drawLine(new Vector3f(), new Vector3f(1f, 1f, 1f), new Vector3f(.5f, 255f, .1f));
        final var drawer = mock(LineDrawer.class);
        debugDraw.draw(drawer);
        verify(drawer).drawLine(1, 1, 2, 2, new Color(.5f, 1f, .1f));
    }

    @Test
    void testDrawContactPoint() {
        assertEquals(1, debugDraw.getHash());
        debugDraw.drawContactPoint(new Vector3f(), new Vector3f(1f, 1f, 1f), 1f, 100, new Vector3f(.5f, .8f, .1f));
        assertNotEquals(1, debugDraw.getHash());
        assertEquals(1, debugDraw.getLineCount());
    }

    @Test
    void testReportErrorWarning() {
        debugDraw.reportErrorWarning("Some warning or error");
    }

    @Test
    void testDraw3dText() {
        debugDraw.draw3dText(new Vector3f(), "Some warning or error");
    }

    @Test
    void testGetDebugMode() {
        assertEquals(DebugDrawModes.DRAW_WIREFRAME | DebugDrawModes.DRAW_AABB | DebugDrawModes.DRAW_CONTACT_POINTS,
            debugDraw.getDebugMode());
    }

    @Test
    void testClear() {
        debugDraw.drawLine(new Vector3f(), new Vector3f(1f, 1f, 1f), new Vector3f(.5f, .8f, .1f));
        assertEquals(1, debugDraw.getLineCount());
        debugDraw.clear();
        assertEquals(0, debugDraw.getLineCount());
    }
}
