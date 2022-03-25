package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;
import java.io.ByteArrayOutputStream;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.data.ByteArrayDataOutput;
import moe.mewore.rabbit.data.SafeDataOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhysicsDummySphereTest {

    @Test
    void testMakeSpheres() {
        final RigidBody mockBody = mock(RigidBody.class);
        final PhysicsDummyBox[] boxes = new PhysicsDummyBox[]{new PhysicsDummyBox(mockBody, 1f, 1f,
            new Vector3f(1f, 1f, 1f), 1f), new PhysicsDummyBox(mockBody, 1f, 1f, new Vector3f(1f, 1f, 1f), 1f)};

        final PhysicsDummySphere[] result = PhysicsDummySphere.makeSpheres(boxes);
        assertEquals(1, result.length);
        assertEquals(new Vector3f(1f, 21f, 1f), result[0].getBody().getWorldTransform(new Transform()).origin);
    }

    @Test
    void testAppendToBinaryOutput() {
        final var byteStream = new ByteArrayOutputStream();
        final SafeDataOutput dataOutput = new ByteArrayDataOutput(byteStream);

        final var body = mock(RigidBody.class);
        when(body.getWorldTransform(any())).thenReturn(new Transform());

        new PhysicsDummySphere(body).appendToBinaryOutput(dataOutput);
        assertEquals(12, byteStream.toByteArray().length);
    }
}
