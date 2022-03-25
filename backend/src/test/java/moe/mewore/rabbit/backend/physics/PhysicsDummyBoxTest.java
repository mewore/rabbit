package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;
import java.io.ByteArrayOutputStream;

import com.bulletphysics.dynamics.RigidBody;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.data.ByteArrayDataOutput;
import moe.mewore.rabbit.data.SafeDataOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PhysicsDummyBoxTest {

    @Test
    void makeBoxes() {
        final PhysicsDummyBox[] result = PhysicsDummyBox.makeBoxes();
        assertEquals(20, result.length);
    }

    @Test
    void testAppendToBinaryOutput() {
        final var byteStream = new ByteArrayOutputStream();
        final SafeDataOutput dataOutput = new ByteArrayDataOutput(byteStream);
        new PhysicsDummyBox(mock(RigidBody.class), 1f, 2f, new Vector3f(1f, 2f, 3f),
            (float) Math.PI).appendToBinaryOutput(dataOutput);

        assertEquals(24, byteStream.toByteArray().length);
    }
}
