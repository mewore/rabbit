package moe.mewore.rabbit.backend.simulation;

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

import lombok.AccessLevel;
import lombok.Getter;
import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.physics.FixedDiscreteDynamicWorld;
import moe.mewore.rabbit.backend.physics.ForestWalls;
import moe.mewore.rabbit.backend.physics.PhysicsDummyBox;
import moe.mewore.rabbit.backend.physics.PhysicsDummySphere;
import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.backend.simulation.data.FrameCompiler;
import moe.mewore.rabbit.backend.simulation.data.FrameDataType;
import moe.mewore.rabbit.backend.simulation.data.FrameSection;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializableEntity;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;
import moe.mewore.rabbit.world.MazeMap;

import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.LONG;

public class RabbitWorldState implements FrameSerializableEntity {

    public static final float GRAVITY = 250f;

    private static final float SECONDS_PER_FRAME = 1f / RealtimeSimulation.FPS;

    private static final float PLAYER_RADIUS = 3f;

    private static final float PLAYER_HEIGHT = 10f;

    private static final ConvexShape PLAYER_SHAPE = new CylinderShape(
        new Vector3f(PLAYER_RADIUS, PLAYER_HEIGHT / 2f, PLAYER_RADIUS));

    private static final float GROUND_HALF_THICKNESS = 100f;

    private static final long PARALLELISM_THRESHOLD = 5L;

    private final ConcurrentHashMap<Integer, Player> players = new ConcurrentHashMap<>();

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
    private long frameId = 0;

    private int playerUid = 0;

    @Getter
    private final int frameSize;

    @Getter(AccessLevel.PACKAGE)
    private final FrameSection headerFrameSection;

    private final FrameSection[] playerControllerFrameSections;

    public RabbitWorldState(final int maxPlayerCount, final MazeMap map) {
        this.maxPlayerCount = maxPlayerCount;
        this.map = map;

        final FrameCompiler frameCompiler = new FrameCompiler();
        headerFrameSection = frameCompiler.reserve(LONG);
        playerControllerFrameSections = frameCompiler.reserveMultiple(maxPlayerCount,
            RigidBodyController.FRAME_DATA_TYPES.toArray(new FrameDataType[0]));

        final CollisionConfiguration configuration = new DefaultCollisionConfiguration();
        world = new FixedDiscreteDynamicWorld(new CollisionDispatcher(configuration), new DbvtBroadphase(),
            new SequentialImpulseConstraintSolver(), configuration);
        world.getPairCache().setInternalGhostPairCallback(new GhostPairCallback());
        world.setGravity(new Vector3f(0f, -GRAVITY, 0f));

        final var planeShape = new BoxShape(new Vector3f(map.getWidth(), GROUND_HALF_THICKNESS, map.getDepth()));
        final var groundPlane = new RigidBody(0f, new DefaultMotionState(), planeShape);
        groundPlane.translate(new Vector3f(0f, -GROUND_HALF_THICKNESS, 0f));
        groundPlane.setCollisionFlags(CollisionFlags.STATIC_OBJECT);
        groundPlane.setFriction(.75f);
        groundPlane.setRestitution(.25f);
        world.addRigidBody(groundPlane);

        boxes = PhysicsDummyBox.makeBoxes();
        for (final PhysicsDummyBox box : boxes) {
            world.addRigidBody(box.getBody());
        }

        spheres = PhysicsDummySphere.makeSpheres(boxes, frameCompiler);
        for (final PhysicsDummySphere sphere : spheres) {
            world.addRigidBody(sphere.getBody());
        }

        for (final var wall : ForestWalls.generate(map).getBodies()) {
            world.addRigidBody(wall);
        }

        frameSize = frameCompiler.getSize();
    }

    public boolean hasPlayers() {
        return !players.isEmpty();
    }

    public Map<Integer, Player> getPlayers() {
        return Collections.unmodifiableMap(players);
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
        body.setFriction(0);
        body.setRestitution(0);
        final var characterController = new RigidBodyController(body, playerControllerFrameSections[id]);
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

    @Override
    public void load(final byte[] frame) {
        headerFrameSection.setFrame(frame);
        frameId = headerFrameSection.readLong();

        players.forEachValue(PARALLELISM_THRESHOLD, player -> player.load(frame));
        for (final PhysicsDummySphere sphere : spheres) {
            sphere.load(frame);
        }
    }

    @Override
    public void store(final byte[] frame) {
        headerFrameSection.setFrame(frame);
        headerFrameSection.writeLong(frameId);

        players.forEachValue(PARALLELISM_THRESHOLD, player -> player.store(frame));
        for (final PhysicsDummySphere sphere : spheres) {
            sphere.store(frame);
        }
    }

    void loadInput(final @Nullable PlayerInputEvent[] frameInputs, final boolean force) {
        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            final @Nullable PlayerInputEvent inputEvent = frameInputs[player.getId()];
            if (inputEvent == null || inputEvent.getPlayerUid() != player.getUid()) {
                if (force) {
                    player.clearInput();
                }
            } else if (force || inputEvent.canReplace(player.getLastInputEvent())) {
                player.applyInput(inputEvent);
            }
        });
    }

    void storeInput(final @Nullable PlayerInputEvent[] frameInputs) {
        players.forEachValue(PARALLELISM_THRESHOLD, player -> {
            final int playerId = player.getId();
            final PlayerInputEvent oldInput = frameInputs[playerId];
            final PlayerInputEvent newInput = player.getLastInputEvent();
            if (newInput != null && newInput.canReplace(oldInput)) {
                frameInputs[playerId] = newInput;
            }
        });
    }
}
