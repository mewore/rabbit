package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.BroadphaseNativeType;
import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.ConcaveShape;
import com.bulletphysics.collision.shapes.ConeShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.PolyhedralConvexShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.TriangleCallback;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.linearmath.IDebugDraw;
import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FixedDiscreteDynamicWorldTest {

    // Mutable, but they should never be changed
    private static final Transform TRANSFORM = new Transform();

    private static final Vector3f COLOR = new Vector3f();

    private IDebugDraw debugDrawer;

    private DiscreteDynamicsWorld world;

    private static <T extends CollisionShape> T makeShape(final Class<T> shapeClass,
        final BroadphaseNativeType shapeType) {
        final T shape = mock(shapeClass);
        when(shape.getShapeType()).thenReturn(shapeType);
        return shape;
    }

    @BeforeEach
    void setUp() {
        debugDrawer = mock(IDebugDraw.class);
        world = new FixedDiscreteDynamicWorld(mock(Dispatcher.class), mock(BroadphaseInterface.class),
            mock(ConstraintSolver.class), mock(CollisionConfiguration.class));
        world.setDebugDrawer(debugDrawer);
    }

    @Test
    void testDebugDrawCompound() {
        final var shape = makeShape(CompoundShape.class, BroadphaseNativeType.COMPOUND_SHAPE_PROXYTYPE);

        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(debugDrawer, never()).drawTriangle(any(), any(), any(), any(), anyFloat());
        verify(debugDrawer, times(3)).drawLine(any(), any(), any());
    }

    @Test
    void testDebugDrawSphere() {
        final var shape = makeShape(SphereShape.class, BroadphaseNativeType.SPHERE_SHAPE_PROXYTYPE);
        when(shape.getMargin()).thenReturn(1f);

        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(debugDrawer, never()).drawTriangle(any(), any(), any(), any(), anyFloat());
        verify(debugDrawer, times(31)).drawLine(any(), any(), any());
    }

    @Test
    void testDebugDrawMultiSphere() {
        final var shape = makeShape(CollisionShape.class, BroadphaseNativeType.MULTI_SPHERE_SHAPE_PROXYTYPE);
        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(debugDrawer, never()).drawTriangle(any(), any(), any(), any(), anyFloat());
        verify(debugDrawer, times(3)).drawLine(any(), any(), any());
    }

    @Test
    void testDebugDrawCapsule() {
        final var shape = makeShape(CapsuleShape.class, BroadphaseNativeType.CAPSULE_SHAPE_PROXYTYPE);
        when(shape.getRadius()).thenReturn(1f);

        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(debugDrawer, never()).drawTriangle(any(), any(), any(), any(), anyFloat());
        verify(debugDrawer, times(63)).drawLine(any(), any(), any());
    }

    @Test
    void testDebugDrawCone() {
        final var shape = makeShape(ConeShape.class, BroadphaseNativeType.CONE_SHAPE_PROXYTYPE);
        when(shape.getRadius()).thenReturn(1f);

        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(debugDrawer, never()).drawTriangle(any(), any(), any(), any(), anyFloat());
        verify(debugDrawer, times(31)).drawLine(any(), any(), any());
    }

    @Test
    void testDebugDrawCylinder() {
        final var shape = makeShape(CylinderShape.class, BroadphaseNativeType.CYLINDER_SHAPE_PROXYTYPE);
        when(shape.getRadius()).thenReturn(1f);

        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(debugDrawer, never()).drawTriangle(any(), any(), any(), any(), anyFloat());
        verify(debugDrawer, times(31)).drawLine(any(), any(), any());
    }

    @Test
    void testDebugDrawOther() {
        final var shape = makeShape(CollisionShape.class, BroadphaseNativeType.IMPLICIT_CONVEX_SHAPES_START_HERE);
        when(shape.isConcave()).thenReturn(false);
        when(shape.isPolyhedral()).thenReturn(false);
        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(debugDrawer, never()).drawTriangle(any(), any(), any(), any(), anyFloat());
        verify(debugDrawer, times(3)).drawLine(any(), any(), any());
    }

    @Test
    void testDebugDrawConcaveMesh() {
        final var shape = makeShape(ConcaveShape.class, BroadphaseNativeType.IMPLICIT_CONVEX_SHAPES_START_HERE);
        when(shape.isConcave()).thenReturn(true);

        final var firstPoint = mock(Vector3f.class);
        final var secondPoint = mock(Vector3f.class);
        final var thirdPoint = mock(Vector3f.class);
        Mockito.doAnswer(invocation -> {
            ((TriangleCallback) invocation.getArgument(0)).processTriangle(
                new Vector3f[]{firstPoint, secondPoint, thirdPoint}, 0, 0);
            return null;
        }).when(shape).processAllTriangles(any(), any(), any());

        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(debugDrawer).drawTriangle(same(firstPoint), same(secondPoint), same(thirdPoint), same(COLOR), eq(1f));
        verify(debugDrawer, times(3)).drawLine(any(), any(), any());
    }

    @Test
    void testDebugDrawPolyhedralMesh() {
        final var shape = makeShape(PolyhedralConvexShape.class, BroadphaseNativeType.CONVEX_SHAPE_PROXYTYPE);
        when(shape.isConcave()).thenReturn(false);
        when(shape.isPolyhedral()).thenReturn(true);
        when(shape.getNumEdges()).thenReturn(2);

        world.debugDrawObject(TRANSFORM, shape, COLOR);
        verify(shape).getEdge(eq(0), any(), any());
        verify(shape).getEdge(eq(1), any(), any());
        verify(debugDrawer, never()).drawTriangle(any(), any(), any(), any(), anyFloat());
        verify(debugDrawer, times(2)).drawLine(any(), any(), same(COLOR));
    }
}
