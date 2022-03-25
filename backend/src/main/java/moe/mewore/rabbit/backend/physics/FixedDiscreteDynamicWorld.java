package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.BroadphaseNativeType;
import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConcaveShape;
import com.bulletphysics.collision.shapes.ConeShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.collision.shapes.PolyhedralConvexShape;
import com.bulletphysics.collision.shapes.TriangleCallback;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.linearmath.Transform;

public class FixedDiscreteDynamicWorld extends DiscreteDynamicsWorld {

    protected int sphereCircleResolution = 8;

    public FixedDiscreteDynamicWorld(final Dispatcher dispatcher, final BroadphaseInterface pairCache,
        final ConstraintSolver constraintSolver, final CollisionConfiguration collisionConfiguration) {
        super(dispatcher, pairCache, constraintSolver, collisionConfiguration);
    }

    private static Vector3f makeGlobal(final Transform worldTransform, final Vector3f vector, final float x,
        final float y, final float z) {
        vector.set(x, y, z);
        worldTransform.basis.transform(vector);
        vector.add(worldTransform.origin);
        return vector;
    }

    @Override
    protected void debugDrawSphere(final float radius, final Transform transform, final Vector3f color) {
        super.debugDrawSphere(radius, transform, color);

        // Currently, it draws only a 2D circle, but it's best if it's made to draw a 3D sphere instead
        final var center = new Vector3f(transform.origin);
        final var to = new Vector3f();
        final var previousTo = new Vector3f();
        final var step = (float) (Math.PI) * 2f / sphereCircleResolution;
        var angle = 0f;
        for (int i = 0; i <= sphereCircleResolution; i++, angle += step) {
            to.set(center.x + (float) Math.cos(angle) * radius, center.y, center.z + (float) Math.sin(angle) * radius);
            if (i > 0) {
                getDebugDrawer().drawLine(center, to, color);
                getDebugDrawer().drawLine(previousTo, to, color);
            }
            previousTo.set(to);
        }
    }

    @Override
    public void debugDrawObject(final Transform worldTransform, final CollisionShape shape, final Vector3f color) {
        super.debugDrawObject(worldTransform, shape, color);

        if (shape.getShapeType() == BroadphaseNativeType.COMPOUND_SHAPE_PROXYTYPE) {
            return;
        }

        switch (shape.getShapeType()) {

            case SPHERE_SHAPE_PROXYTYPE: {
                final var radius = shape.getMargin(); // radius doesn't include the margin, so draw with margin
                debugDrawSphere(radius, worldTransform, color);
                break;
            }
            case MULTI_SPHERE_SHAPE_PROXYTYPE: {
                break;
            }
            case CAPSULE_SHAPE_PROXYTYPE: {
                final var capsuleShape = (CapsuleShape) shape;
                final float radius = capsuleShape.getRadius();
                final float halfHeight = capsuleShape.getHalfHeight();
                final var localTransform = new Transform();
                localTransform.setIdentity();
                final var childTransform = new Transform();

                // Draw the ends
                localTransform.origin.set(0f, halfHeight, 0f);
                childTransform.mul(worldTransform, localTransform);
                debugDrawSphere(radius, childTransform, color);

                localTransform.origin.set(0f, -halfHeight, 0f);
                childTransform.mul(worldTransform, localTransform);
                debugDrawSphere(radius, childTransform, color);

                // Draw the cylinder lines
                final var from = new Vector3f();
                final var to = new Vector3f();

                getDebugDrawer().drawLine(makeGlobal(worldTransform, from, -radius, halfHeight, 0),
                    makeGlobal(worldTransform, to, -radius, -halfHeight, 0), color);
                getDebugDrawer().drawLine(makeGlobal(worldTransform, from, radius, halfHeight, 0),
                    makeGlobal(worldTransform, to, radius, -halfHeight, 0), color);

                getDebugDrawer().drawLine(makeGlobal(worldTransform, from, 0, halfHeight, -radius),
                    makeGlobal(worldTransform, to, 0, -halfHeight, -radius), color);
                getDebugDrawer().drawLine(makeGlobal(worldTransform, from, 0, halfHeight, radius),
                    makeGlobal(worldTransform, to, 0, -halfHeight, radius), color);
                break;
            }
            case CONE_SHAPE_PROXYTYPE: {
                // An upright cone looks circular, like a sphere, when looked from above.
                // However, this needs to be updated to work in 3D too.
                debugDrawSphere(((ConeShape) shape).getRadius(), worldTransform, color);
                break;

            }
            case CYLINDER_SHAPE_PROXYTYPE: {
                // An upright cylinder looks circular, like a sphere, when looked from above.
                // However, this needs to be updated to work in 3D too.
                debugDrawSphere(((CylinderShape) shape).getRadius(), worldTransform, color);
                break;
            }
            default: {

                if (shape.isConcave()) {
                    final var aabbMax = new Vector3f(1e30f, 1e30f, 1e30f);
                    final var aabbMin = new Vector3f(-1e30f, -1e30f, -1e30f);

                    ((ConcaveShape) shape).processAllTriangles(new TriangleCallback() {
                        @Override
                        public void processTriangle(final Vector3f[] triangle, final int partId,
                            final int triangleIndex) {
                            getDebugDrawer().drawTriangle(triangle[0], triangle[1], triangle[2], color, 1f);
                        }
                    }, aabbMin, aabbMax);
                    return;
                }

                if (shape.isPolyhedral()) {
                    final var first = new Vector3f();
                    final var second = new Vector3f();
                    final var polyhedralShape = ((PolyhedralConvexShape) shape);
                    int i;
                    for (i = 0; i < polyhedralShape.getNumEdges(); i++) {
                        polyhedralShape.getEdge(i, first, second);
                        worldTransform.transform(first);
                        worldTransform.transform(second);
                        getDebugDrawer().drawLine(first, second, color);
                    }
                }
            }
        }
    }
}
