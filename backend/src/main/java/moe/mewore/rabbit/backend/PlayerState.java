package moe.mewore.rabbit.backend;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.geometry.Vector2;
import moe.mewore.rabbit.geometry.Vector3;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PlayerState extends BinaryEntity {

    private static final double MAX_SPEED = 80;

    private static final double ACCELERATION = 200;

    private final Vector3 position;

    private final Vector3 motion;

    private final Vector2 targetHorizontalMotion;

    private int inputId;

    private float inputAngle;

    private int inputKeys;

    PlayerState() {
        this(new Vector3(), new Vector3(), new Vector2());
    }

    public void applyInput(final int inputId, final float inputAngle, final int inputKeys) {
        if (this.inputId > inputId) {
            return;
        }
        this.inputId = inputId;
        this.inputAngle = inputAngle;
        this.inputKeys = inputKeys;

        final boolean up = (inputKeys & PlayerInputMutation.INPUT_UP_BIT) > 0;
        final boolean down = (inputKeys & PlayerInputMutation.INPUT_DOWN_BIT) > 0;
        final boolean left = (inputKeys & PlayerInputMutation.INPUT_LEFT_BIT) > 0;
        final boolean right = (inputKeys & PlayerInputMutation.INPUT_RIGHT_BIT) > 0;
        if (up || down || left || right) {
            final double angle = PlayerInputMutation.getTargetAngle(inputAngle, up, down, left, right);
            targetHorizontalMotion.set(Math.cos(angle) * MAX_SPEED, Math.sin(angle) * MAX_SPEED);
        } else {
            targetHorizontalMotion.set(0, 0);
        }
    }

    public void applyTime(final double dt) {
        final double motionDx = targetHorizontalMotion.getX() - motion.getX();
        final double motionDz = targetHorizontalMotion.getY() - motion.getZ();
        final double motionDistanceSquared = motionDx * motionDx + motionDz * motionDz;
        final double maxAcceleration = ACCELERATION * dt;
        if (motionDistanceSquared > maxAcceleration * maxAcceleration) {
            final double multiplier = maxAcceleration / Math.sqrt(motionDistanceSquared);
            motion.add(motionDx * multiplier, 0, motionDz * multiplier);
        } else {
            motion.set(targetHorizontalMotion.getX(), motion.getY(), targetHorizontalMotion.getY());
        }

        position.add(motion.getX() * dt, motion.getY() * dt, motion.getZ() * dt);
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        position.appendToBinaryOutput(output);
        motion.appendToBinaryOutput(output);
    }
}
