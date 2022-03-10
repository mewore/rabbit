package moe.mewore.rabbit.backend;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jetty.websocket.api.Session;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsBinaryMessageHandler;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsCloseHandler;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsConnectHandler;
import io.javalin.websocket.WsContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.editor.EditorVersionHandler;
import moe.mewore.rabbit.backend.messages.HeartbeatRequest;
import moe.mewore.rabbit.backend.messages.MapDataMessage;
import moe.mewore.rabbit.backend.messages.PlayerDisconnectMessage;
import moe.mewore.rabbit.backend.messages.PlayerJoinMessage;
import moe.mewore.rabbit.backend.messages.WorldUpdateMessage;
import moe.mewore.rabbit.backend.mutations.HeartbeatResponse;
import moe.mewore.rabbit.backend.mutations.MutationType;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;
import moe.mewore.rabbit.backend.mutations.PlayerJoinMutation;
import moe.mewore.rabbit.backend.net.MultiPlayerHeart;
import moe.mewore.rabbit.backend.simulation.WorldSimulation;
import moe.mewore.rabbit.backend.simulation.WorldState;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.noise.CompositeNoise;
import moe.mewore.rabbit.noise.DiamondSquareNoise;
import moe.mewore.rabbit.noise.Noise;
import moe.mewore.rabbit.world.MazeMap;
import moe.mewore.rabbit.world.WorldProperties;

// This is the server, so of course it's "overly coupled".
@SuppressWarnings("OverlyCoupledClass")
@RequiredArgsConstructor
public class Server implements WsConnectHandler, WsBinaryMessageHandler, WsCloseHandler {

    private final ScheduledExecutorService simulationThread = Executors.newSingleThreadScheduledExecutor();

    private static final int MAXIMUM_NUMBER_OF_PLAYERS = 10;

    private static final int UPDATES_PER_SECOND = 10;

    private final Map<String, Player> playerBySessionId = new ConcurrentHashMap<>();

    private final Map<Integer, Session> sessionByPlayerId = new ConcurrentHashMap<>();

    private final Map<String, Session> sessionById = new ConcurrentHashMap<>();

    private final ServerSettings serverSettings;

    private final Javalin javalin;

    @Getter
    private final MazeMap map;

    @Getter
    private final WorldState worldState = new WorldState(MAXIMUM_NUMBER_OF_PLAYERS);

    private final WorldSimulation worldSimulation = new WorldSimulation(worldState);

    private final AtomicReference<@NonNull ServerState> state = new AtomicReference<>(ServerState.STOPPED);

    private final ScheduledExecutorService heartbeatThread = Executors.newSingleThreadScheduledExecutor();

    private final MultiPlayerHeart heart = new MultiPlayerHeart(MAXIMUM_NUMBER_OF_PLAYERS, this::sendHeartbeat);

    private final List<Consumer<WorldState>> worldUpdateListeners = new ArrayList<>();

    public static Server create(final ServerSettings settings) throws IOException {
        final @Nullable String externalStaticLocation = settings.getExternalStaticLocation();
        final Javalin javalin = Javalin.create(config -> {
            config.addStaticFiles("static");
            if (externalStaticLocation != null) {
                config.addStaticFiles(externalStaticLocation, Location.EXTERNAL);
            }
        });

        final WorldProperties worldProperties = WorldProperties.getFromClasspath();
        final long seed = worldProperties.getSeedAsLong();
        final Noise opennessNoise = new CompositeNoise(
            DiamondSquareNoise.createSeamless(worldProperties.getNoiseResolution(), new Random(seed), 1.0,
                worldProperties.getNoiseSharpness()),
            DiamondSquareNoise.createSeamless(worldProperties.getNoiseResolution(), new Random(seed + 1), 1.0,
                worldProperties.getNoiseSharpness()), CompositeNoise.XNOR_BLENDING);

        final MazeMap map = MazeMap.createSeamless(worldProperties, new Random(seed), opennessNoise);

        javalin.get("editors", externalStaticLocation != null
            ? new EditorVersionHandler(externalStaticLocation)
            : ctx -> ctx.json(Collections.emptySet()));

        final Server server = new Server(settings, javalin, map);
        javalin.ws("/multiplayer", ws -> {
            ws.onConnect(server);
            ws.onBinaryMessage(server);
            ws.onClose(server);
        });
        return server;
    }

    public static void main(final String[] args) throws IOException {
        create(new ServerSettings(args, System.getenv())).start();
    }

    private void setServerState(final ServerState expectedInitialState, final ServerState resultingState) {
        final ServerState oldValue = state.getAndUpdate(
            initialState -> initialState == expectedInitialState ? resultingState : initialState);
        if (oldValue != expectedInitialState) {
            throw new IllegalStateException(
                "The server expected to transition from state " + expectedInitialState + " to " + resultingState +
                    " but its state was " + oldValue);
        }
    }

    public void onWorldUpdate(final Consumer<WorldState> handler) {
        worldUpdateListeners.add(handler);
    }

    public Server start() {
        setServerState(ServerState.STOPPED, ServerState.STARTING);

        simulationThread.scheduleAtFixedRate(() -> {
            try {
                final WorldState newState = worldSimulation.step();
                if (newState.hasPlayers()) {
                    broadcast(new WorldUpdateMessage(newState));
                }
                for (final Consumer<WorldState> handler : worldUpdateListeners) {
                    handler.accept(newState);
                }
            } catch (final RuntimeException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }, 0, 1000L / UPDATES_PER_SECOND, TimeUnit.MILLISECONDS);
        heartbeatThread.scheduleAtFixedRate(() -> {
            try {
                heart.doStep();
            } catch (final RuntimeException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }, 0, heart.getStepTimeInterval(), TimeUnit.MILLISECONDS);
        javalin.start(serverSettings.getPort());

        setServerState(ServerState.STARTING, ServerState.RUNNING);
        return this;
    }

    public void stop() throws InterruptedException {
        setServerState(ServerState.RUNNING, ServerState.STOPPING);

        try {
            final var tasksToStop = new ScheduledExecutorService[]{simulationThread, heartbeatThread};
            for (final ScheduledExecutorService task : tasksToStop) {
                task.shutdown();
            }
            if (!simulationThread.awaitTermination(1, TimeUnit.MINUTES)) {
                System.err.println("The simulation thread was not terminated even after waiting for 1 minute");
            }
            if (!heartbeatThread.awaitTermination(1, TimeUnit.MINUTES)) {
                System.err.println("The heartbeat thread was not terminated even after waiting for 1 minute");
            }
        } finally {
            javalin.stop();
        }

        setServerState(ServerState.STOPPING, ServerState.STOPPED);

    }

    @Override
    public void handleBinaryMessage(final WsBinaryMessageContext sender) throws IOException {
        final @Nullable Player player = playerBySessionId.get(sender.getSessionId());
        final DataInput dataInput = new DataInputStream(new ByteArrayInputStream(sender.data()));
        final byte mutationTypeIndex = dataInput.readByte();
        final MutationType mutationType = Arrays.stream(MutationType.values())
            .filter(type -> type.getIndex() == mutationTypeIndex)
            .findAny()
            .orElseThrow(
                () -> new IllegalArgumentException("There is no mutation type with index " + mutationTypeIndex));
        switch (mutationType) {
            case PLAYER_JOIN:
                if (player != null) {
                    throw new IllegalArgumentException(
                        "There is already a player for session " + sender.getSessionId() + "! Cannot join again.");
                }
                handleJoin(sender, PlayerJoinMutation.decodeFromBinary(dataInput));
                return;
            case PLAYER_INPUT:
                if (player == null) {
                    throw new IllegalArgumentException("There is no player for session " + sender.getSessionId());
                }
                handleInput(player, PlayerInputMutation.decodeFromBinary(dataInput));
                return;
            case HEARTBEAT_RESPONSE:
                if (player != null) {
                    final HeartbeatResponse response = HeartbeatResponse.decodeFromBinary(dataInput);
                    heart.receive(player, response.getId());
                }
        }
    }

    @Override
    public void handleConnect(final @NonNull WsConnectContext sender) {
        sender.send(ByteBuffer.wrap(new MapDataMessage(map).encodeToBinary()));
        for (final Player player : playerBySessionId.values()) {
            sender.send(ByteBuffer.wrap(new PlayerJoinMessage(player, false).encodeToBinary()));
        }
        sessionById.put(sender.getSessionId(), sender.session);
    }

    private enum ServerState {
        STOPPED,
        STOPPING,
        STARTING,
        RUNNING
    }

    private void sendHeartbeat(final int playerId, final int heartbeatId) {
        final @Nullable Session session = sessionByPlayerId.get(playerId);
        if (session != null && session.isOpen()) {
            session.getRemote().sendBytesByFuture(ByteBuffer.wrap(new HeartbeatRequest(heartbeatId).encodeToBinary()));
        }
    }

    private synchronized void handleJoin(final WsContext sender, final PlayerJoinMutation joinMutation) {
        final @Nullable Player newPlayer = worldState.createPlayer(joinMutation.isReisen());
        // TODO: Change the joining to a normal HTTP request so that there can be a [400] response
        if (newPlayer != null) {
            playerBySessionId.put(sender.getSessionId(), newPlayer);
            sessionByPlayerId.put(newPlayer.getId(), sender.session);
            broadcast(sender, new PlayerJoinMessage(newPlayer, false));
            sender.send(ByteBuffer.wrap(new PlayerJoinMessage(newPlayer, true).encodeToBinary()));
            heart.addPlayer(newPlayer);
        } else {
            System.err.println("Failed to create player!");
        }
    }

    private void handleInput(final Player player, final PlayerInputMutation updateMutation) {
        worldSimulation.acceptInput(player, updateMutation);
    }

    @Override
    public void handleClose(final WsCloseContext sender) {
        sessionById.remove(sender.getSessionId());
        final @Nullable Player player = playerBySessionId.remove(sender.getSessionId());
        if (player != null) {
            worldState.removePlayer(player);
            broadcast(sender, new PlayerDisconnectMessage(player));
            heart.removePlayer(player);
            sessionByPlayerId.remove(player.getId());
        }
    }

    private void broadcast(final BinaryEntity entityToBroadcast) {
        broadcast(null, entityToBroadcast);
    }

    private void broadcast(final @Nullable WsContext context, final BinaryEntity entityToBroadcast) {
        final byte[] dataToBroadcast = entityToBroadcast.encodeToBinary();

        sessionById.values()
            .stream()
            .filter(context != null ? session -> session != context.session && session.isOpen() : Session::isOpen)
            .forEach(session -> session.getRemote().sendBytesByFuture(ByteBuffer.wrap(dataToBroadcast)));
    }
}
