package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.QuaternionUtil;
import com.bulletphysics.linearmath.Transform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class PhysicsDummyBox extends BinaryEntity {

    private static final float ROTATION_PER_BOX = (float) (Math.PI * 0.15);

    private static final float[] HEIGHT_INCREASES = new float[]{10, 15, 18, -20};

    private static final int DUMMY_BOX_COUNT = 20;

    @Getter
    private final RigidBody body;

    private final float width;

    private final float height;

    @Getter
    private final Vector3f position;

    private final float rotationY;

    public static PhysicsDummyBox[] makeBoxes() {
        final var physicsDummyBoxAcceleration = -3f;
        float physicsDummyBoxSpeed = -25f;
        final var position = new Vector3f(-30, 0, -30);
        float dummyBoxHeight = 10f;
        final PhysicsDummyBox[] result = new PhysicsDummyBox[DUMMY_BOX_COUNT];

        final var boxTransform = new Transform();
        boxTransform.setIdentity();
        final var tmpQuaternion = new Quat4f();
        for (int i = 0; i < DUMMY_BOX_COUNT; i++) {
            final float width = 15 + (i == 9 ? 15 : 0) + (i == DUMMY_BOX_COUNT - 1 ? 25 : 0);

            var currentHeight = dummyBoxHeight;
            var y = currentHeight / 2;
            if (currentHeight > width * 3) {
                y = currentHeight;
                currentHeight = width * 0.25f;
                y -= currentHeight / 2f;
            }
            final var currentPosition = new Vector3f(position);
            currentPosition.y = y;
            final var currentRotation = (i - 0.5f) * ROTATION_PER_BOX;

            final var shape = new BoxShape(new Vector3f(width / 2f, currentHeight / 2f, width / 2f));
            QuaternionUtil.setEuler(tmpQuaternion, currentRotation, 0f, 0f);
            boxTransform.origin.set(currentPosition);
            boxTransform.setRotation(tmpQuaternion);
            final var constructionInfo = new RigidBodyConstructionInfo(0f, new DefaultMotionState(boxTransform), shape);
            final RigidBody body = new RigidBody(constructionInfo);
            body.setCollisionFlags(CollisionFlags.STATIC_OBJECT);

            final float currentSpeed = i == 8 ? physicsDummyBoxSpeed * 1.5f : physicsDummyBoxSpeed;
            final var movementAngle = i * ROTATION_PER_BOX + (float) Math.PI / 2;
            position.set(position.x + (float) Math.sin(movementAngle) * currentSpeed, position.y,
                position.z + (float) Math.cos(movementAngle) * currentSpeed);
            physicsDummyBoxSpeed += physicsDummyBoxAcceleration;

            dummyBoxHeight += HEIGHT_INCREASES[i % HEIGHT_INCREASES.length];
            result[i] = new PhysicsDummyBox(body, width, currentHeight, currentPosition, currentRotation);
        }
        return result;
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeFloat(width);
        output.writeFloat(height);
        output.writeFloat(position.x);
        output.writeFloat(position.y);
        output.writeFloat(position.z);
        output.writeFloat(rotationY);
    }
}
