package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    void setUp() {
        body = mock(RigidBody.class);
        controller = new RigidBodyController(body);
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

    @Test
    void testGetPosition() {
        final Transform transform = new Transform();
        when(body.getWorldTransform(any())).thenReturn(transform);
        assertSame(transform.origin, controller.getPosition());
    }

    @Test
    void testSetPosition() {
        controller.setPosition(new Vector3f(1f, 1f, 1f));
        verify(body).setWorldTransform(any());
    }

    @Test
    void testLoadPosition() {
        controller.loadPosition(new float[]{0f, 1f, 2f, 3f, 4f}, 1);
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
    void testGetMotion_noTarget() {
        final Vector3f result = new Vector3f();
        when(body.getLinearVelocity(any())).thenReturn(result);
        assertSame(result, controller.getMotion());
    }

    @Test
    void testSetMotion() {
        final Vector3f target = new Vector3f();
        controller.setMotion(target);
        verify(body).setLinearVelocity(same(target));
    }

    @Test
    void testLoadMotion() {
        controller.loadPosition(new float[]{0f, 1f, 2f, 3f, 4f}, 1);
        verify(body).setWorldTransform(any());
    }
}
