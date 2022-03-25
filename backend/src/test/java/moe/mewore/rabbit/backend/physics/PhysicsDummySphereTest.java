package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;
import java.io.ByteArrayOutputStream;

import com.bulletphysics.collision.shapes.TriangleShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.data.ByteArrayDataOutput;
import moe.mewore.rabbit.data.SafeDataOutput;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void testLoad() {
        final var sphere = new PhysicsDummySphere(new RigidBody(1, new DefaultMotionState(), new TriangleShape()));
        final var intData = new int[]{0, 0, 5, 0};
        final var floatData = new float[]{0, 1, 2, 3, 4, 5, 6, 0};
        assertNotEquals(5, sphere.getBody().getActivationState());
        assertEquals(new Vector3f(0, 0, 0), sphere.getBody().getWorldTransform(new Transform()).origin);
        assertEquals(new Vector3f(0, 0, 0), sphere.getBody().getLinearVelocity(new Vector3f()));
        sphere.load(intData, 2, floatData, 1);
        assertEquals(5, sphere.getBody().getActivationState());
        assertEquals(new Vector3f(1, 2, 3), sphere.getBody().getWorldTransform(new Transform()).origin);
        assertEquals(new Vector3f(4, 5, 6), sphere.getBody().getLinearVelocity(new Vector3f()));
    }

    @Test
    void testStore() {
        final var sphere = new PhysicsDummySphere(new RigidBody(1, new DefaultMotionState(), new TriangleShape()));
        final var intData = new int[4];
        final var floatData = new float[8];
        final var transform = new Transform();

        sphere.getBody().forceActivationState(5);
        transform.setIdentity();
        transform.origin.set(1, 2, 3);
        sphere.getBody().setWorldTransform(transform);
        sphere.getBody().setLinearVelocity(new Vector3f(4, 5, 6));

        sphere.store(intData, 2, floatData, 1);
        assertArrayEquals(new int[]{0, 0, 5, 0}, intData);
        assertArrayEquals(new float[]{0, 1, 2, 3, 4, 5, 6, 0}, floatData);
    }

    @Test
    void testAppendToBinaryOutput() {
        final var byteStream = new ByteArrayOutputStream();
        final SafeDataOutput dataOutput = new ByteArrayDataOutput(byteStream);

        final var body = mock(RigidBody.class);
        when(body.getActivationState()).thenReturn(5);
        when(body.getWorldTransform(any())).thenReturn(new Transform());
        when(body.getLinearVelocity(any())).thenReturn(new Vector3f());

        new PhysicsDummySphere(body).appendToBinaryOutput(dataOutput);
        assertEquals(25, byteStream.toByteArray().length);
    }
}
