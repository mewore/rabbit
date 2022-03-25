package moe.mewore.rabbit.backend.simulation;

import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.dispatch.GhostPairCallback;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.collision.shapes.CylinderShape;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.Synchronized;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;
import moe.mewore.rabbit.backend.physics.FixedDiscreteDynamicWorld;
import moe.mewore.rabbit.backend.physics.ForestWalls;
import moe.mewore.rabbit.backend.physics.PhysicsDummyBox;
import moe.mewore.rabbit.backend.physics.PhysicsDummySphere;
import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.world.MazeMap;

public class WorldState extends BinaryEntity {

    public static final float GRAVITY = 250f;

    // int(uid), int(input ID) + [5 x boolean](input directions, wants to jump)
    private static int INT_DATA_PER_PLAYER = 0;

    private static final int PLAYER_UID_OFFSET = INT_DATA_PER_PLAYER++;

    private static final int PLAYER_INPUT_ID_OFFSET = INT_DATA_PER_PLAYER++;

    private static final int PLAYER_INPUT_KEYS_OFFSET = INT_DATA_PER_PLAYER++;

    private static final float SECONDS_PER_FRAME = 1f / WorldSimulation.FPS;

    private static final float PLAYER_RADIUS = 3f;

    private static final float PLAYER_HEIGHT = 10f;

    private static final ConvexShape PLAYER_SHAPE = new CylinderShape(
        new Vector3f(PLAYER_RADIUS, PLAYER_HEIGHT / 2f, PLAYER_RADIUS));

    private static final float GROUND_HALF_THICKNESS = 100f;

    // Vector3f + Vector3f + [input angle]
    private static int FLOAT_DATA_PER_PLAYER = 0;

    private static final int PLAYER_INPUT_ANGLE_OFFSET = FLOAT_DATA_PER_PLAYER++;

    private static final int PLAYER_POSITION_OFFSET = makePlayerFloatField(3);

    private static final int PLAYER_MOTION_OFFSET = makePlayerFloatField(3);

    private static final int PLAYER_GROUND_TIME_LEFT_OFFSET = FLOAT_DATA_PER_PLAYER++;

    private static final int PLAYER_JUMP_CONTROL_TIME_LEFT_OFFSET = FLOAT_DATA_PER_PLAYER++;

    private static final long PARALLELISM_THRESHOLD = 5L;

    private final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

    public static final int BYTES_PER_STORED_STATE = (INT_DATA_PER_PLAYER + FLOAT_DATA_PER_PLAYER) * 4 + 2 * 8 + 8;

    @Getter
    private final DynamicsWorld world;

    @Getter
    private final PhysicsDummyBox[] boxes;

    @Getter
    private final int maxPlayerCount;

    private final MazeMap map;

    @Getter
    private final PhysicsDummySphere[] spheres;

    @Getter
    private int frameId = 0;

    private int playerUid = 0;

    public WorldState(final int maxPlayerCount, final MazeMap map) {
        this.maxPlayerCount = maxPlayerCount;
        this.map = map;

        final CollisionConfiguration configuration = new DefaultCollisionConfiguration();
        world = new FixedDiscreteDynamicWorld(new CollisionDispatcher(configuration), new DbvtBroadphase(),
            new SequentialImpulseConstraintSolver(), configuration);
        world.getPairCache().setInternalGhostPairCallback(new GhostPairCallback());
        world.setGravity(new Vector3f(0f, -GRAVITY, 0f));

        final var planeShape = new BoxShape(new Vector3f(map.getWidth(), GROUND_HALF_THICKNESS, map.getDepth()));
        final var groundPlane = new RigidBody(0f, new DefaultMotionState(), planeShape);
        groundPlane.translate(new Vector3f(0f, -GROUND_HALF_THICKNESS, 0f));
        groundPlane.setCollisionFlags(CollisionFlags.STATIC_OBJECT);
        groundPlane.setRestitution(.5f);
        world.addRigidBody(groundPlane);

        boxes = PhysicsDummyBox.makeBoxes();
        for (final PhysicsDummyBox box : boxes) {
            world.addRigidBody(box.getBody());
        }

        spheres = PhysicsDummySphere.makeSpheres(boxes);
        for (final PhysicsDummySphere sphere : spheres) {
            world.addRigidBody(sphere.getBody());
        }

        for (final var wall : ForestWalls.generate(map).getBodies()) {
            world.addRigidBody(wall);
        }
    }

    public static void registerInput(final WorldSnapshot snapshot, final Player player,
        final PlayerInputMutation input) {
        final int[] intData = snapshot.getIntData();
        final float[] floatData = snapshot.getFloatData();

        final int intIndex = player.getId() * INT_DATA_PER_PLAYER;
        final int oldPlayerUid = intData[intIndex + PLAYER_UID_OFFSET];
        if (player.getUid() > oldPlayerUid || input.getId() >= intData[intIndex + PLAYER_INPUT_ID_OFFSET]) {
            intData[intIndex + PLAYER_INPUT_ID_OFFSET] = input.getId();
            intData[intIndex + PLAYER_INPUT_KEYS_OFFSET] = input.getKeys();
            floatData[player.getId() * FLOAT_DATA_PER_PLAYER + PLAYER_INPUT_ANGLE_OFFSET] = input.getAngle();
        } else {
            System.out.printf(
                "Not storing input #%d for player with UID %d; the old player UID is %d and the old input ID is #%d%n",
                input.getId(), player.getUid(), oldPlayerUid, intData[intIndex + PLAYER_INPUT_ID_OFFSET]);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static int makePlayerFloatField(final int size) {
        final int result = FLOAT_DATA_PER_PLAYER;
        FLOAT_DATA_PER_PLAYER += size;
        return result;
    }

    public boolean hasPlayers() {
        return !players.isEmpty();
    }

    public Map<Integer, Player> getPlayers() {
        return Collections.unmodifiableMap(players);
    }

    private static void storeTuple3f(final Tuple3f tuple, final float[] floatData, final int index) {
        floatData[index] = tuple.x;
        floatData[index + 1] = tuple.y;
        floatData[index + 2] = tuple.z;
    }

    public @Nullable Player createPlayer(final boolean isReisen) {
        final AtomicReference<Player> result = new AtomicReference<>();
        final Function<Integer, Player> mappingFunction = (key) -> {
            result.set(createPlayer(key, isReisen));
            return result.get();
        };
        for (int i = 0; i < maxPlayerCount; i++) {
            players.computeIfAbsent(i, mappingFunction);
            if (result.get() != null) {
                return result.get();
            }
        }
        return null;
    }

    private Player createPlayer(final int id, final boolean isReisen) {
        final Transform startTransform = new Transform();
        startTransform.setIdentity();
        startTransform.origin.y = PLAYER_HEIGHT / 2f;

        final var body = new RigidBody(
            new RigidBodyConstructionInfo(1f, new DefaultMotionState(startTransform), PLAYER_SHAPE));
        final var characterController = new RigidBodyController(body);
        world.addRigidBody(body);

        return new Player(++playerUid, id, "Player " + (id + 1), isReisen, world, body, characterController);
    }

    /**
     * @param player The player to remove
     * @return Whether the player was in this world in the first place.
     */
    public boolean removePlayer(final Player player) {
        world.removeCollisionObject(player.getBody());
        world.removeAction(player.getCharacterController());
        return players.remove(player.getId(), player);
    }

    WorldSnapshot createEmptySnapshot() {
        return new WorldSnapshot(
            maxPlayerCount * INT_DATA_PER_PLAYER + spheres.length * PhysicsDummySphere.INT_DATA_PER_SPHERE,
            maxPlayerCount * FLOAT_DATA_PER_PLAYER + spheres.length * PhysicsDummySphere.FLOAT_DATA_PER_SPHERE);
    }

    void doStep() {
        players.forEachValue(PARALLELISM_THRESHOLD, player -> player.beforePhysics(SECONDS_PER_FRAME));
        try {
            world.stepSimulation(SECONDS_PER_FRAME, 0, SECONDS_PER_FRAME);
        } catch (final NullPointerException e) {
            System.err.println("Error encountered while simulating frame " + (frameId + 1) + ": " + e.getMessage());
            e.printStackTrace();
        }
        players.forEachValue(PARALLELISM_THRESHOLD, player -> player.afterPhysics(map));
        ++frameId;
    }

    void load(final WorldSnapshot snapshot, final int snapshotFrameId) {
        frameId = snapshotFrameId;

        final int[] intData = snapshot.getIntData();
        final float[] floatData = snapshot.getFloatData();

        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            final int intIndex = player.getId() * INT_DATA_PER_PLAYER;
            final int uid = intData[intIndex + PLAYER_UID_OFFSET];
            if (player.getUid() != uid) {
                return;
            }

            final int floatIndex = player.getId() * FLOAT_DATA_PER_PLAYER;
            player.getInputState()
                .applyInput(intData[intIndex + PLAYER_INPUT_ID_OFFSET],
                    floatData[floatIndex + PLAYER_INPUT_ANGLE_OFFSET], intData[intIndex + PLAYER_INPUT_KEYS_OFFSET]);

            final var controller = player.getCharacterController();
            controller.loadPosition(floatData, floatIndex + PLAYER_POSITION_OFFSET);
            controller.loadMotion(floatData, floatIndex + PLAYER_MOTION_OFFSET);
            controller.groundTimeLeft = floatData[floatIndex + PLAYER_GROUND_TIME_LEFT_OFFSET];
            controller.jumpControlTimeLeft = floatData[floatIndex + PLAYER_JUMP_CONTROL_TIME_LEFT_OFFSET];
        });

        int sphereIntIndex = maxPlayerCount * INT_DATA_PER_PLAYER;
        int sphereFloatIndex = maxPlayerCount * FLOAT_DATA_PER_PLAYER;
        for (final PhysicsDummySphere sphere : spheres) {
            sphere.load(intData, sphereIntIndex, floatData, sphereFloatIndex);
            sphereIntIndex += PhysicsDummySphere.INT_DATA_PER_SPHERE;
            sphereFloatIndex += PhysicsDummySphere.FLOAT_DATA_PER_SPHERE;
        }
    }

    void loadInput(final WorldSnapshot snapshot) {
        final int[] intData = snapshot.getIntData();
        final float[] floatData = snapshot.getFloatData();

        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            final int intIndex = player.getId() * INT_DATA_PER_PLAYER;
            if (player.getUid() != intData[intIndex + PLAYER_UID_OFFSET]) {
                return;
            }

            final int floatIndex = player.getId() * FLOAT_DATA_PER_PLAYER;
            player.getInputState()
                .applyInput(intData[intIndex + PLAYER_INPUT_ID_OFFSET],
                    floatData[floatIndex + PLAYER_INPUT_ANGLE_OFFSET], intData[intIndex + PLAYER_INPUT_KEYS_OFFSET]);
        });
    }

    void store(final WorldSnapshot snapshot) {
        final int[] intData = snapshot.getIntData();
        final float[] floatData = snapshot.getFloatData();

        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            final int intIndex = player.getId() * INT_DATA_PER_PLAYER;
            final int floatIndex = player.getId() * FLOAT_DATA_PER_PLAYER;
            final boolean playerIsNew = player.getUid() > intData[intIndex + PLAYER_UID_OFFSET];
            intData[intIndex + PLAYER_UID_OFFSET] = player.getUid();
            if (playerIsNew || player.getInputState().getInputId() >= intData[intIndex + PLAYER_INPUT_ID_OFFSET]) {
                intData[intIndex + PLAYER_INPUT_ID_OFFSET] = player.getInputState().getInputId();
                intData[intIndex + PLAYER_INPUT_KEYS_OFFSET] = player.getInputState().getInputKeys();
                floatData[floatIndex + PLAYER_INPUT_ANGLE_OFFSET] = player.getInputState().getInputAngle();
            }

            final var controller = player.getCharacterController();
            storeTuple3f(controller.getPosition(), floatData, floatIndex + PLAYER_POSITION_OFFSET);
            storeTuple3f(controller.getMotion(), floatData, floatIndex + PLAYER_MOTION_OFFSET);
            floatData[floatIndex + PLAYER_GROUND_TIME_LEFT_OFFSET] = controller.groundTimeLeft;
            floatData[floatIndex + PLAYER_JUMP_CONTROL_TIME_LEFT_OFFSET] = controller.jumpControlTimeLeft;
        });

        int sphereIntIndex = maxPlayerCount * INT_DATA_PER_PLAYER;
        int sphereFloatIndex = maxPlayerCount * FLOAT_DATA_PER_PLAYER;
        for (final PhysicsDummySphere sphere : spheres) {
            sphere.store(intData, sphereIntIndex, floatData, sphereFloatIndex);
            sphereIntIndex += PhysicsDummySphere.INT_DATA_PER_SPHERE;
            sphereFloatIndex += PhysicsDummySphere.FLOAT_DATA_PER_SPHERE;
        }
    }

    @Synchronized
    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(frameId);
        output.writeInt(players.size());
        players.forEachEntry(Long.MAX_VALUE, entry -> entry.getValue().appendToBinaryOutput(output));
        output.writeInt(spheres.length);
        for (final var sphere : spheres) {
            sphere.appendToBinaryOutput(output);
        }
    }
}
