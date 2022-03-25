import { BulletCollisionFlags } from './bullet-collision-flags';

/**
 * For some reason, when using a collision mask, it has to be shifted one bit to the left
 * of the collision filter it's supposed to match.
 */
export enum BulletCollisionMasks {
    /**
     * Dynamic objects move at every step and usually collide with static, kinematic and other dynamic objects.
     */
    DYNAMIC_OBJECTS = BulletCollisionFlags.DYNAMIC_OBJECT << 1,

    /**
     * Static objects never move.
     */
    STATIC_OBJECTS = BulletCollisionFlags.STATIC_OBJECT << 1,

    /**
     * Kinematic objects are moved manually through code.
     */
    KINEMATIC_OBJECTS = BulletCollisionFlags.KINEMATIC_OBJECT << 1,

    /**
     * Character objects are... complicated.
     */
    CHARACTER_OBJECTS = BulletCollisionFlags.CHARACTER_OBJECT << 1,

    /**
     * Objects that break the camera's line of sight.
     */
    CAMERA_BLOCKING_OBJECTS = BulletCollisionFlags.CAMERA_BLOCKING_OBJECT << 1,

    /**
     * A mask that matches everything.
     */
    ALL = 0xffff,
}
