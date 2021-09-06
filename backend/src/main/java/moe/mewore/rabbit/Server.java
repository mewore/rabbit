package moe.mewore.rabbit;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.NotNull;

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
import moe.mewore.rabbit.entities.BinaryEntity;
import moe.mewore.rabbit.entities.Player;
import moe.mewore.rabbit.entities.PlayerState;
import moe.mewore.rabbit.entities.events.PlayerConnectEvent;
import moe.mewore.rabbit.entities.events.PlayerDisconnectEvent;
import moe.mewore.rabbit.entities.events.PlayerUpdateEvent;

@RequiredArgsConstructor
public class Server implements WsConnectHandler, WsBinaryMessageHandler, WsCloseHandler {

    private final AtomicInteger nextPlayerId = new AtomicInteger();

    private final ServerSettings serverSettings;

    private final Javalin javalin;

    private final Map<String, Player> playerBySessionId = new HashMap<>();

    private final Map<String, Session> sessionById = new HashMap<>();

    public static void main(final String[] args) {
        start(new ServerSettings(args, System.getenv()));
    }

    static Javalin start(final ServerSettings settings) {
        final Javalin javalin = Javalin.create(config -> {
            config.addStaticFiles("static");
            final @Nullable String externalStaticLocation = settings.getExternalStaticLocation();
            if (externalStaticLocation != null) {
                config.addStaticFiles(settings.getExternalStaticLocation(), Location.EXTERNAL);
            }
        });
        return new Server(settings, javalin).start();
    }

    private Javalin start() {
        javalin.ws("/multiplayer", ws -> {
            ws.onConnect(this);
            ws.onBinaryMessage(this);
            ws.onClose(this);
        });
        return javalin.start(serverSettings.getPort());
    }

    @Override
    public void handleConnect(@NotNull final WsConnectContext sender) {
        final int playerId = nextPlayerId.incrementAndGet();
        final String username = "Player " + playerId;
        final Player newPlayer = new Player(playerId, username);
        broadcast(sender, new PlayerConnectEvent(newPlayer));
        for (final Player player : playerBySessionId.values()) {
            sender.send(ByteBuffer.wrap(new PlayerConnectEvent(player).encodeToBinary()));
        }
        playerBySessionId.put(sender.getSessionId(), newPlayer);
        sessionById.put(sender.getSessionId(), sender.session);
    }

    @Override
    public void handleBinaryMessage(@NotNull final WsBinaryMessageContext sender) throws IOException {
        final @Nullable Player player = playerBySessionId.get(sender.getSessionId());
        if (player == null) {
            throw new IllegalArgumentException("There is no session with ID " + sender.getSessionId());
        }
        final PlayerState newState = PlayerState.decodeFromBinary(
            new DataInputStream(new ByteArrayInputStream(sender.data())));
        player.setState(newState);
        broadcast(sender, new PlayerUpdateEvent(player.getId(), newState));
    }

    @Override
    public void handleClose(@NotNull final WsCloseContext sender) {
        final @Nullable Player player = playerBySessionId.get(sender.getSessionId());
        if (player == null) {
            throw new IllegalArgumentException("There is no session with ID " + sender.getSessionId());
        }
        playerBySessionId.remove(sender.getSessionId());
        sessionById.remove(sender.getSessionId());
        broadcast(sender, new PlayerDisconnectEvent(player));
    }

    private void broadcast(final WsContext context, final BinaryEntity entityToBroadcast) {
        final byte[] dataToBroadcast = entityToBroadcast.encodeToBinary();

        sessionById.values()
            .stream()
            .filter(session -> session != context.session && session.isOpen())
            .forEach(session -> session.getRemote().sendBytesByFuture(ByteBuffer.wrap(dataToBroadcast)));
    }
}
