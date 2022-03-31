package moe.mewore.rabbit.backend.game;

import javax.vecmath.Vector3f;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
import moe.mewore.rabbit.backend.physics.FixedDiscreteDynamicWorld;
import moe.mewore.rabbit.backend.physics.ForestWalls;
import moe.mewore.rabbit.backend.physics.PhysicsDummyBox;
import moe.mewore.rabbit.backend.physics.PhysicsDummySphere;
import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.backend.simulation.WorldBase;
import moe.mewore.rabbit.backend.simulation.data.FrameDataType;
import moe.mewore.rabbit.backend.simulation.data.FrameSection;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializableEntity;
import moe.mewore.rabbit.world.MazeMap;

public class RabbitWorld extends WorldBase<RabbitPlayerInput, RabbitPlayer> {

    public static final float GRAVITY = 250f;

    private static final float PLAYER_RADIUS = 3f;

    private static final float PLAYER_HEIGHT = 10f;

    private static final ConvexShape PLAYER_SHAPE = new CylinderShape(
        new Vector3f(PLAYER_RADIUS, PLAYER_HEIGHT / 2f, PLAYER_RADIUS));

    private static final float GROUND_HALF_THICKNESS = 100f;

    private static final long PARALLELISM_THRESHOLD = 5L;

    private final ConcurrentHashMap<Integer, RabbitPlayer> players = new ConcurrentHashMap<>();

    @Getter
    private final DynamicsWorld physicsWorld;

    @Getter
    private final PhysicsDummyBox[] boxes;

    private final MazeMap map;

    @Getter
    private final PhysicsDummySphere[] spheres;

    private final FrameSection[] playerControllerFrameSections;

    public RabbitWorld(final int maxPlayerCount, final MazeMap map, final DynamicsWorld physicsWorld) {
        super(maxPlayerCount);
        this.map = map;
        this.physicsWorld = physicsWorld;

        playerControllerFrameSections = frameCompiler.reserveMultiple(maxPlayerCount,
            RigidBodyController.FRAME_DATA_TYPES.toArray(new FrameDataType[0]));
        boxes = PhysicsDummyBox.makeBoxes();
        spheres = PhysicsDummySphere.makeSpheres(boxes, frameCompiler);

        afterPlayerRemoval = player -> {
            physicsWorld.removeCollisionObject(player.getBody());
            players.remove(player.getIndex(), player);
        };
    }

    public static DynamicsWorld createPhysicsWorld() {
        final CollisionConfiguration configuration = new DefaultCollisionConfiguration();
        final DynamicsWorld physicsWorld = new FixedDiscreteDynamicWorld(new CollisionDispatcher(configuration),
            new DbvtBroadphase(), new SequentialImpulseConstraintSolver(), configuration);
        physicsWorld.getPairCache().setInternalGhostPairCallback(new GhostPairCallback());
        physicsWorld.setGravity(new Vector3f(0f, -GRAVITY, 0f));
        return physicsWorld;
    }

    public void initialize() {
        final var planeShape = new BoxShape(new Vector3f(map.getWidth(), GROUND_HALF_THICKNESS, map.getDepth()));
        final var groundPlane = new RigidBody(0f, new DefaultMotionState(), planeShape);
        groundPlane.translate(new Vector3f(0f, -GROUND_HALF_THICKNESS, 0f));
        groundPlane.setCollisionFlags(CollisionFlags.STATIC_OBJECT);
        groundPlane.setFriction(.75f);
        groundPlane.setRestitution(.25f);
        physicsWorld.addRigidBody(groundPlane);

        for (final PhysicsDummyBox box : boxes) {
            physicsWorld.addRigidBody(box.getBody());
        }

        for (final PhysicsDummySphere sphere : spheres) {
            physicsWorld.addRigidBody(sphere.getBody());
        }

        for (final var wall : ForestWalls.generate(map).getBodies()) {
            physicsWorld.addRigidBody(wall);
        }
    }

    @Override
    public Map<Integer, RabbitPlayer> getPlayersAsMap() {
        return Collections.unmodifiableMap(players);
    }

    @Override
    protected void forEachSerializableEntity(final Consumer<FrameSerializableEntity> consumer) {
        forEachPlayer(consumer::accept);
        for (final PhysicsDummySphere sphere : spheres) {
            consumer.accept(sphere);
        }
    }

    @Synchronized
    public @Nullable RabbitPlayer createPlayer(final boolean isReisen) {
        final @Nullable Integer index = reservePlayerIndex();
        if (index == null) {
            return null;
        }

        final Transform startTransform = new Transform();
        startTransform.setIdentity();
        startTransform.origin.y = RabbitWorld.PLAYER_HEIGHT / 2f;

        final var body = new RigidBody(
            new RigidBodyConstructionInfo(1f, new DefaultMotionState(startTransform), RabbitWorld.PLAYER_SHAPE));
        body.setFriction(0);
        body.setRestitution(0);
        final var characterController = new RigidBodyController(body, playerControllerFrameSections[index]);
        physicsWorld.addRigidBody(body);

        final RabbitPlayer player = new RabbitPlayer(nextPlayerUid(), index, "Player " + (index + 1), isReisen,
            physicsWorld, body, characterController);
        players.put(index, player);
        return player;
    }

    @Override
    public void forEachPlayer(final Consumer<RabbitPlayer> playerConsumer) {
        players.forEachValue(PARALLELISM_THRESHOLD, playerConsumer);
    }

    @Override
    public void doStep(final float deltaSeconds) {
        players.forEachValue(PARALLELISM_THRESHOLD, player -> player.beforePhysics(deltaSeconds));
        try {
            physicsWorld.stepSimulation(deltaSeconds, 0, deltaSeconds);
        } catch (final NullPointerException e) {
            System.err.println("Error encountered while simulating frame " + (frameId + 1) + ": " + e.getMessage());
            e.printStackTrace();
        }
        players.forEachValue(PARALLELISM_THRESHOLD, player -> player.afterPhysics(map));
        ++frameId;
    }
}
