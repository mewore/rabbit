package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.util.ObjectArrayList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.geometry.Vector2;
import moe.mewore.rabbit.world.MazeMap;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ForestWalls {

    private static final float HEIGHT = 500f;

    private static final float HALF_HEIGHT = HEIGHT / 2f;

    @Getter
    private final RigidBody[] bodies;

    public static ForestWalls generate(final MazeMap map) {
        final float worldWidth = map.getWidth();
        final float worldDepth = map.getDepth();
        final float halfWorldWidth = worldWidth * .5f;
        final float halfWorldDepth = worldDepth * .5f;
        final Vector3f tmpVector3 = new Vector3f();

        final RigidBody[] wallBodies = map.getWalls().stream().map(wall -> {
            final var points = wall.getPolygon().getPoints();
            if (points.size() == 4) {
                // Rectangle -> box
                final float minX = points.stream().map(Vector2::getX).min(Float::compare).orElse(0f) * worldWidth;
                final float minY = points.stream().map(Vector2::getY).min(Float::compare).orElse(0f) * worldDepth;
                final float maxX =
                    points.stream().map(Vector2::getX).max(Float::compare).orElse(minX + 1f) * worldWidth;
                final float maxY =
                    points.stream().map(Vector2::getY).max(Float::compare).orElse(minY + 1f) * worldDepth;
                tmpVector3.set((maxX - minX) * .5f, HALF_HEIGHT, (maxY - minY) * .5f);
                final var shape = new BoxShape(tmpVector3);
                final var body = new RigidBody(0f, new DefaultMotionState(), shape);
                tmpVector3.set((maxX + minX) * .5f - halfWorldWidth, HALF_HEIGHT, (maxY + minY) * .5f - halfWorldDepth);
                body.translate(tmpVector3);
                return body;
            }
            // Polygon -> polyhedron

            // The average position of the points will be the center
            final float xSum = (float) points.stream().mapToDouble(Vector2::getX).sum();
            final float ySum = (float) points.stream().mapToDouble(Vector2::getY).sum();
            tmpVector3.set(xSum * worldWidth / points.size(), HALF_HEIGHT, ySum * worldDepth / points.size());

            final ObjectArrayList<Vector3f> shapePoints = new ObjectArrayList<>(points.size() * 2);
            for (final Vector2 point : points) {
                final float x = point.getX() * worldWidth - tmpVector3.x;
                final float z = point.getY() * worldDepth - tmpVector3.z;
                shapePoints.add(new Vector3f(x, -HALF_HEIGHT, z));
                shapePoints.add(new Vector3f(x, HALF_HEIGHT, z));
            }
            final var shape = new ConvexHullShape(shapePoints);
            final var body = new RigidBody(0f, new DefaultMotionState(), shape);

            tmpVector3.x -= halfWorldWidth;
            tmpVector3.z -= halfWorldDepth;
            body.translate(tmpVector3);
            return body;
        }).toArray(RigidBody[]::new);

        for (final var body : wallBodies) {
            body.setCollisionFlags(CollisionFlags.STATIC_OBJECT);
            body.setFriction(.7f);
            body.setRestitution(0);
        }

        return new ForestWalls(wallBodies);
    }
}
