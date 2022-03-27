package moe.mewore.rabbit.backend;

import javax.vecmath.Vector2f;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PlayerInputState {

    private static final float MAX_SPEED = 100f;

    @Getter
    private final Vector2f targetHorizontalMotion;

    private int inputId;

    private float inputAngle;

    private int inputKeys;

    private boolean jumping;

    PlayerInputState() {
        this(new Vector2f());
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
            targetHorizontalMotion.set((float) (Math.cos(angle) * MAX_SPEED), (float) (Math.sin(angle) * MAX_SPEED));
        } else {
            targetHorizontalMotion.set(0, 0);
        }

        jumping = (inputKeys & PlayerInputMutation.INPUT_JUMP_BIT) > 0;
    }
}
