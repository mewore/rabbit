package moe.mewore.rabbit.backend;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.backend.editor.EditorVersionHandler;
import moe.mewore.rabbit.backend.messages.MapDataMessage;
import moe.mewore.rabbit.backend.messages.PlayerDisconnectMessage;
import moe.mewore.rabbit.backend.messages.PlayerJoinMessage;
import moe.mewore.rabbit.backend.messages.WorldUpdateMessage;
import moe.mewore.rabbit.backend.mutations.MutationType;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;
import moe.mewore.rabbit.backend.mutations.PlayerJoinMutation;
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

    private static final int UPDATES_PER_SECOND = 10;

    private final Map<String, Player> playerBySessionId = new ConcurrentHashMap<>();

    private final Map<String, Session> sessionById = new ConcurrentHashMap<>();

    private final ServerSettings serverSettings;

    private final Javalin javalin;

    private final MazeMap map;

    private final WorldState worldState = new WorldState(10);

    private final WorldSimulation worldSimulation = new WorldSimulation(worldState);

    private final ScheduledExecutorService timeStepThread = Executors.newSingleThreadScheduledExecutor();

    public static void main(final String[] args) throws IOException {
        start(new ServerSettings(args, System.getenv()));
    }

    static Javalin start(final ServerSettings settings) throws IOException {
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

        return new Server(settings, javalin, map).start();
    }

    private Javalin start() {
        javalin.ws("/multiplayer", ws -> {
            ws.onConnect(this);
            ws.onBinaryMessage(this);
            ws.onClose(this);
        });
        timeStepThread.scheduleAtFixedRate(() -> {
            try {
                final WorldState newState = worldSimulation.step();
                if (newState.hasPlayers()) {
                    broadcast(new WorldUpdateMessage(newState));
                }
            } catch (final RuntimeException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }, 0, 1000L / UPDATES_PER_SECOND, TimeUnit.MILLISECONDS);
        return javalin.start(serverSettings.getPort());
    }

    @Override
    public void handleConnect(final @NonNull WsConnectContext sender) {
        sender.send(ByteBuffer.wrap(new MapDataMessage(map).encodeToBinary()));
        for (final Player player : playerBySessionId.values()) {
            sender.send(ByteBuffer.wrap(new PlayerJoinMessage(player, false).encodeToBinary()));
        }
        sessionById.put(sender.getSessionId(), sender.session);
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
        }
    }

    private synchronized void handleJoin(final WsContext sender, final PlayerJoinMutation joinMutation) {
        final @Nullable Player newPlayer = worldState.createPlayer(joinMutation.isReisen());
        // TODO: Change the joining to a normal HTTP request so that there can be a [400] response
        if (newPlayer != null) {
            playerBySessionId.put(sender.getSessionId(), newPlayer);
            broadcast(sender, new PlayerJoinMessage(newPlayer, false));
            sender.send(ByteBuffer.wrap(new PlayerJoinMessage(newPlayer, true).encodeToBinary()));
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
