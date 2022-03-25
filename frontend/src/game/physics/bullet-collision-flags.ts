/**
 * Originally defined at: BulletCollision/CollisionDispatch/btCollisionObject.h - enum CollisionFlags
 *
 * @see https://pybullet.org/Bullet/BulletFull/btCollisionObject_8h_source.html
 */
export enum BulletCollisionFlags {
    /**
     * Dynamic objects move at every step and usually collide with static, kinematic and other dynamic objects.
     */
    DYNAMIC_OBJECT = 0,

    /**
     * Static objects never move.
     */
    STATIC_OBJECT = 1,

    /**
     * Kinematic objects are moved manually through code.
     */
    KINEMATIC_OBJECT = 1 << 1,

    /**
     * Disables contact response.
     */
    NO_CONTACT_RESPONSE = 1 << 2,

    /**
     * This allows per-triangle material (friction/restitution).
     */
    CUSTOM_MATERIAL_CALLBACK = 1 << 3,

    /**
     * Character objects are... complicated.
     */
    CHARACTER_OBJECT = 1 << 4,

    /**
     * Disables debug drawing.
     */
    DISABLE_VISUALIZE_OBJECT = 1 << 5,

    /**
     * Disables parallel/SPU processing
     */
    DISABLE_SPU_COLLISION_PROCESSING = 1 << 6,

    HAS_CONTACT_STIFFNESS_DAMPING = 1 << 7,

    HAS_CUSTOM_DEBUG_RENDERING_COLOR = 1 << 8,

    HAS_FRICTION_ANCHOR = 1 << 9,

    HAS_COLLISION_SOUND_TRIGGER = 1 << 10,

    /**
     * Objects that break the camera's line of sight.
     */
    CAMERA_BLOCKING_OBJECT = 1 << 11,

    /**
     * A mask that matches everything.
     */
    ALL_OBJECT_TYPES = 0xffff,
}
