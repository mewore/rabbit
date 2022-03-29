package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.simulation.data.FrameCompiler;
import moe.mewore.rabbit.backend.simulation.data.FrameDataType;
import moe.mewore.rabbit.backend.simulation.data.FrameSection;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializationTestUtil;

import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.FLOAT;
import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.VECTOR3F;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RigidBodyControllerTest {

    private static final FrameDataType[] FRAME_DATA_TYPES = new FrameDataType[]{VECTOR3F, VECTOR3F, FLOAT, FLOAT};

    private static final RigidBody OTHER_BODY = mock(RigidBody.class);

    private RigidBody body;

    private RigidBodyController controller;

    private static void setWorldCollisions(final CollisionWorld world, final PersistentManifold... collisionPairs) {
        final var dispatcher = mock(Dispatcher.class);
        when(world.getDispatcher()).thenReturn(dispatcher);
        when(dispatcher.getNumManifolds()).thenReturn(collisionPairs.length);

        for (int i = 0; i < collisionPairs.length; i++) {
            when(dispatcher.getManifoldByIndexInternal(i)).thenReturn(collisionPairs[i]);
        }
    }

    private static PersistentManifold makeCollisionPair(final RigidBody firstBody, final RigidBody secondBody,
        final float... normalYCoordinates) {
        final var collisionPair = mock(PersistentManifold.class);
        when(collisionPair.getNumContacts()).thenReturn(normalYCoordinates.length);
        when(collisionPair.getBody0()).thenReturn(firstBody);
        when(collisionPair.getBody1()).thenReturn(secondBody);
        for (int i = 0; i < normalYCoordinates.length; i++) {
            final var contact = new ManifoldPoint(new Vector3f(), new Vector3f(),
                new Vector3f(0f, normalYCoordinates[i], 0f), 1f);
            when(collisionPair.getContactPoint(i)).thenReturn(contact);
        }
        return collisionPair;
    }

    private static RigidBody makeBody(final float x, final float y, final float z, final float vx, final float vy,
        final float vz) {
        final var transform = new Transform();
        transform.origin.set(x, y, z);

        final var result = new RigidBody(1f, new DefaultMotionState(transform), mock(CollisionShape.class));

        result.setLinearVelocity(new Vector3f(vx, vy, vz));
        return result;
    }

    @Test
    void testUpdateAction() {
        final var world = mock(CollisionWorld.class);

        final var motion = new Vector3f();
        when(body.getLinearVelocity(any())).thenReturn(motion);
        controller.updateAction(world, .1f);
        verify(body).setLinearVelocity(same(motion));
        verify(body, never()).activate();
    }

    @Test
    void testUpdateAction_moving() {
        final var world = mock(CollisionWorld.class);

        controller.getTargetHorizontalMotion().x = 10000f;
        when(body.getLinearVelocity(any())).thenReturn(new Vector3f());
        controller.updateAction(world, .1f);
        verify(body).activate();
        verify(body).setLinearVelocity(eq(new Vector3f(40f, 0f, 0f)));
    }

    @Test
    void testUpdateAction_jump() {
        final var world = mock(CollisionWorld.class);

        // Make it so that the character is on the ground
        setWorldCollisions(world, makeCollisionPair(body, OTHER_BODY, 1f));
        controller.afterPhysics(world);

        controller.jump();

        when(body.getLinearVelocity(any())).thenReturn(new Vector3f());
        controller.updateAction(world, .1f);

        verify(body).setLinearVelocity(eq(new Vector3f(0f, 110f, 0f)));
    }

    @Test
    void testAfterPhysics() {
        assertFalse(controller.onGround());

        final var world = mock(CollisionWorld.class);
        setWorldCollisions(world, makeCollisionPair(body, OTHER_BODY, 1f));
        controller.afterPhysics(world);
        assertTrue(controller.onGround());
    }

    @Test
    void testAfterPhysics_notOnGround() {
        final var world = mock(CollisionWorld.class);
        setWorldCollisions(world, makeCollisionPair(body, OTHER_BODY, 0f));
        controller.afterPhysics(world);
        assertFalse(controller.onGround());
    }

    @Test
    void testAfterPhysics_manyCollisions() {
        final var world = mock(CollisionWorld.class);
        setWorldCollisions(world, makeCollisionPair(body, OTHER_BODY, 1f, 0f), makeCollisionPair(OTHER_BODY, body, 3f),
            makeCollisionPair(OTHER_BODY, OTHER_BODY, 5f));
        controller.afterPhysics(world);
    }

    @BeforeEach
    void setUp() {
        body = mock(RigidBody.class);
        controller = new RigidBodyController(body, mock(FrameSection.class));
    }

    @Test
    void testSetPosition() {
        controller.setPosition(new Vector3f(1f, 1f, 1f));
        verify(body).setWorldTransform(any());
    }

    @Test
    void testGetMotion() {
        final Vector3f target = new Vector3f();
        final Vector3f result = new Vector3f();
        when(body.getLinearVelocity(same(target))).thenReturn(result);
        assertSame(result, controller.getMotion(target));
    }

    @Test
    void testGetPosition() {
        final Transform transform = new Transform();
        when(body.getWorldTransform(any())).thenReturn(transform);
        assertSame(transform.origin, controller.getPosition(new Transform()));
    }

    @Test
    void testSetMotion() {
        final Vector3f target = new Vector3f();
        controller.setMotion(target);
        verify(body).setLinearVelocity(same(target));
    }

    @Test
    void testGetMotion_noTarget() {
        final Vector3f result = new Vector3f();
        when(body.getLinearVelocity(any())).thenReturn(result);
        assertSame(result, controller.getMotion(new Vector3f()));
    }

    @Test
    void testSerialization() {
        final var frameCompiler = new FrameCompiler();
        final FrameSection frameSection = frameCompiler.reserve(RigidBodyController.FRAME_DATA_TYPES);

        final var firstController = new RigidBodyController(makeBody(1, 2, 3, 4, 5, 6), frameSection);
        final var otherController = new RigidBodyController(makeBody(7, 8, 9, 0, 1, 2), frameSection);

        FrameSerializationTestUtil.testSerialization(frameCompiler, frameSection, firstController, otherController);
    }
}
