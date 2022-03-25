export enum BulletDebugDrawModes {
    /**
     * Do not draw anything.
     */
    NO_DEBUG = 0,

    /**
     * Draw a wireframe of the collision shapes.
     */
    DRAW_WIREFRAME = 1,

    /**
     * Draw the axis-aligned bounding boxes of the collision shapes.
     */
    DRAW_AABB = 2,

    DRAW_FEATURES_TEXT = 4,

    /**
     * Upon collision, draw the collision contact points.
     */
    DRAW_CONTACT_POINTS = 8,

    NO_DEACTIVATION = 16,
    NO_HELP_TEXT = 32,
    DRAW_TEXT = 64,
    PROFILE_TIMINGS = 128,
    ENABLE_SAT_COMPARISON = 256,
    DISABLE_BULLET_LCP = 512,
    ENABLE_CCD = 1024,
    DRAW_CONSTRAINTS = 1 << 11,
    DRAW_CONSTRAINT_LIMITS = 1 << 12,
    FAST_WIREFRAME = 1 << 13,
    DRAW_NORMALS = 1 << 14,
    DRAW_FRAMES = 1 << 15,
    MAX_DEBUG_DRAW_MODE = 0xffffffff,
}
