package moe.mewore.rabbit.backend.preview;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PlayerPreview {

    private final int x;

    private final int y;

    private final int motionX;

    private final int motionY;

    private final int targetMotionX;

    private final int targetMotionY;

    private final String topText;

    private final String bottomText;

    public boolean hasMotionLine() {
        return motionX != x || motionY != y;
    }

    public boolean hasTargetMotionLine() {
        return (targetMotionX != x || targetMotionY != y) && (targetMotionX != motionX || targetMotionY != motionY);
    }
}
