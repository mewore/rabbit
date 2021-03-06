package moe.mewore.rabbit.backend;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.javalin.Javalin;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import lombok.SneakyThrows;
import moe.mewore.rabbit.backend.game.RabbitPlayer;
import moe.mewore.rabbit.backend.game.RabbitPlayerInput;
import moe.mewore.rabbit.backend.game.RabbitWorld;
import moe.mewore.rabbit.backend.messages.MessageType;
import moe.mewore.rabbit.backend.mock.FakeMap;
import moe.mewore.rabbit.backend.mock.ws.FakeWsSession;
import moe.mewore.rabbit.backend.mutations.MutationType;
import moe.mewore.rabbit.backend.physics.PhysicsDummyBox;
import moe.mewore.rabbit.backend.physics.PhysicsDummySphere;
import moe.mewore.rabbit.backend.simulation.RealtimeSimulation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerTest {

    private static final PhysicsDummyBox[] NO_BOXES = new PhysicsDummyBox[0];

    private Server server;

    private RabbitWorld world;

    @Mock
    private RealtimeSimulation<RabbitPlayerInput> worldSimulation;

    private RabbitPlayer firstPlayer;

    private RabbitPlayer secondPlayer;

    private ScheduledExecutorService threadPool;

    private Javalin javalin;

    @BeforeEach
    void setUp() {
        javalin = mock(Javalin.class);
        firstPlayer = mock(RabbitPlayer.class);
        secondPlayer = mock(RabbitPlayer.class);
        world = mock(RabbitWorld.class);
        threadPool = mock(ScheduledExecutorService.class);
        server = new Server(new ServerSettings(new String[0], Map.of()), javalin, new FakeMap(), world, worldSimulation,
            threadPool);
    }

    @Test
    void testStart() {
        server.start();

        final ArgumentCaptor<Long> rateCaptor = ArgumentCaptor.forClass(Long.class);
        verify(threadPool, times(2)).scheduleAtFixedRate(any(), eq(0L), rateCaptor.capture(),
            same(TimeUnit.MILLISECONDS));
        assertEquals(List.of(100L, 33L), rateCaptor.getAllValues());

        verify(javalin).start(8100);
    }

    @Test
    void testStart_alreadyRunning() {
        server.start();
        final var exception = assertThrows(IllegalStateException.class, server::start);
        assertEquals("The server expected to transition from state STOPPED to STARTING but its state was RUNNING",
            exception.getMessage());
    }

    @Test
    void testStop() throws InterruptedException {
        server.start();
        when(threadPool.awaitTermination(1L, TimeUnit.MINUTES)).thenReturn(true);
        server.stop();

        verify(threadPool).shutdown();
        verify(javalin).stop();
    }

    @Test
    void testStop_failureToTerminateThreadPool() throws InterruptedException {
        server.start();
        when(threadPool.awaitTermination(anyLong(), any())).thenReturn(false);
        server.stop();

        verify(javalin).stop();
    }

    @Test
    void testStop_notRunning() {
        final var exception = assertThrows(IllegalStateException.class, server::stop);
        assertEquals("The server expected to transition from state RUNNING to STOPPING but its state was STOPPED",
            exception.getMessage());
    }

    @Test
    void testHandleConnect() {
        when(world.getBoxes()).thenReturn(NO_BOXES);
        final var session = new FakeWsSession("session");
        simulateConnect(session);
        assertEquals(List.of(MessageType.MAP_DATA), session.getSentMessageTypes());
    }

    @Test
    void testHandleConnect_afterJoin() {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);
        when(world.createPlayer(anyBoolean())).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("");
        simulateJoin(session);

        final var otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN), otherSession.getSentMessageTypes());
    }

    @Test
    void testHandleBinaryMessage_join() throws IOException {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);

        final var otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        when(world.createPlayer(anyBoolean())).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("12345");
        when(firstPlayer.isReisen()).thenReturn(true);
        simulateJoin(session, true);

        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN), session.getSentMessageTypes());
        final DataInput eventInput = new DataInputStream(new ByteArrayInputStream(session.getSentData().get(1)));
        assertEquals(MessageType.JOIN.getIndex(), eventInput.readByte());
        assertEquals(0, eventInput.readInt()); // playerId
        assertEquals(5, eventInput.readInt()); // username length
        final byte[] stringBytes = new byte[5];
        eventInput.readFully(stringBytes);
        assertEquals("12345", new String(stringBytes, StandardCharsets.US_ASCII));
        assertTrue(eventInput.readBoolean()); // isReisen
        assertTrue(eventInput.readBoolean()); // isSelf

        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN), otherSession.getSentMessageTypes());
        final DataInput otherEventInput = new DataInputStream(
            new ByteArrayInputStream(otherSession.getSentData().get(1)));
        assertEquals(MessageType.JOIN.getIndex(), otherEventInput.readByte());
        assertEquals(0, otherEventInput.readInt()); // playerId
        assertEquals(5, otherEventInput.readInt()); // username length
        otherEventInput.readFully(stringBytes);
        assertEquals("12345", new String(stringBytes, StandardCharsets.US_ASCII));
        assertTrue(otherEventInput.readBoolean()); // isReisen
        assertFalse(otherEventInput.readBoolean()); // isSelf
    }

    @Test
    void testHandleBinaryMessage_join_failedToCreatePlayer() {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);
        simulateJoin(session, true);
        assertEquals(List.of(MessageType.MAP_DATA), session.getSentMessageTypes());
    }

    @Test
    void testHandleBinaryMessage_playerInput() throws InterruptedException {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);
        when(world.createPlayer(anyBoolean())).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("");
        simulateJoin(session);

        final var otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MutationType.PLAYER_INPUT.getIndex());
        byteArrayOutputStream.writeBytes(new byte[3 * 4 + 8]);
        simulateBinaryData(session, byteArrayOutputStream.toByteArray());

        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN), otherSession.getSentMessageTypes());
        // The new state is not sent immediately; instead, it is sent at every world step

        verify(worldSimulation).acceptInput(any(), any());
    }

    @Test
    void testHandleBinaryMessage_playerInput_interrupted() throws InterruptedException {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);
        when(world.createPlayer(anyBoolean())).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("");
        simulateJoin(session);

        Mockito.doThrow(new InterruptedException("oof")).when(worldSimulation).acceptInput(any(), any());
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MutationType.PLAYER_INPUT.getIndex());
        byteArrayOutputStream.writeBytes(new byte[3 * 4 + 8]);

        final var thread = spy(new Thread(() -> simulateBinaryData(session, byteArrayOutputStream.toByteArray())));
        thread.start();
        thread.join();
        verify(thread).interrupt();
    }

    @Test
    void testHandleBinaryMessage_heartbeat() {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);
        when(world.createPlayer(anyBoolean())).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("");
        simulateJoin(session);

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MutationType.HEARTBEAT_RESPONSE.getIndex());
        byteArrayOutputStream.writeBytes(new byte[4]);
        simulateBinaryData(session, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testHandleBinaryMessage_heartbeat_noPlayer() {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MutationType.HEARTBEAT_RESPONSE.getIndex());
        byteArrayOutputStream.writeBytes(new byte[4]);
        simulateBinaryData(session, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testHandleBinaryMessage_invalidMutationType() {
        when(world.getBoxes()).thenReturn(NO_BOXES);
        final var session = new FakeWsSession("session");
        simulateConnect(session);
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> simulateBinaryData(session, new byte[]{25}));
        assertEquals("There is no mutation type with index 25", exception.getMessage());
    }

    @Test
    void testHandleBinaryMessage_join_alreadyConnected() {
        final var session = new FakeWsSession("session");
        when(world.createPlayer(anyBoolean())).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("");
        simulateJoin(session);
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> simulateBinaryData(session, new byte[]{MutationType.PLAYER_JOIN.getIndex()}));
        assertEquals("There is already a player for session session! Cannot join again.", exception.getMessage());
    }

    @Test
    void testHandleBinaryMessage_update_notConnected() {
        final var session = new FakeWsSession("session");
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> simulateBinaryData(session, new byte[]{MutationType.PLAYER_INPUT.getIndex()}));
        assertEquals("There is no player for session session", exception.getMessage());
    }

    @Test
    void testHandleClose() {
        when(world.getBoxes()).thenReturn(NO_BOXES);
        final var firstSession = new FakeWsSession("session");
        simulateConnect(firstSession);
        simulateClose(firstSession);
        assertEquals(List.of(MessageType.MAP_DATA), firstSession.getSentMessageTypes());
    }

    @Test
    void testHandleClose_two() throws IOException {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);
        when(world.createPlayer(anyBoolean())).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("");
        simulateJoin(session);

        final var otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        simulateClose(session);

        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN, MessageType.DISCONNECT),
            otherSession.getSentMessageTypes());
        final DataInput eventInput = new DataInputStream(new ByteArrayInputStream(otherSession.getSentData().get(2)));
        assertEquals(MessageType.DISCONNECT.getIndex(), eventInput.readByte());
        assertEquals(0, eventInput.readInt());
    }

    @Test
    void testHandleClose_notConnected() {
        when(world.getBoxes()).thenReturn(NO_BOXES);
        final var session = new FakeWsSession("session");
        simulateConnect(session);
        simulateClose(session);
        simulateClose(session);
    }

    @Test
    void testUpdateWorld() {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);
        when(world.createPlayer(false)).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("");
        when(firstPlayer.getLatency()).thenReturn(200);
        simulateJoin(session, false);

        final var otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        when(world.createPlayer(true)).thenReturn(secondPlayer);
        when(secondPlayer.getIndex()).thenReturn(1);
        when(secondPlayer.getUsername()).thenReturn("");
        when(secondPlayer.getLatency()).thenReturn(2000);
        simulateJoin(otherSession, true);

        final var sessionWithNoPlayer = new FakeWsSession("no-player");
        simulateConnect(sessionWithNoPlayer);

        final AtomicReference<RabbitWorld> worldFromUpdate = new AtomicReference<>();
        server.onWorldUpdate(worldFromUpdate::set);

        when(worldSimulation.getCurrentSnapshot()).thenReturn(new byte[0]);
        when(worldSimulation.getPastSnapshot(anyInt())).thenReturn(new byte[0]);
        when(world.getSpheres()).thenReturn(new PhysicsDummySphere[0]);
        server.updateWorld();
        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN, MessageType.JOIN, MessageType.UPDATE),
            session.getSentMessageTypes());
        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN, MessageType.JOIN, MessageType.UPDATE),
            otherSession.getSentMessageTypes());

        assertSame(world, worldFromUpdate.get());
        verify(worldSimulation).update(anyLong());
        verify(worldSimulation).getPastSnapshot(300);
        verify(worldSimulation).getPastSnapshot(3000);
    }

    @Test
    void testSendHeartbeat() {
        final var session = new FakeWsSession("session");
        when(world.getBoxes()).thenReturn(NO_BOXES);
        simulateConnect(session);
        when(world.createPlayer(false)).thenReturn(firstPlayer);
        when(firstPlayer.getUsername()).thenReturn("");
        simulateJoin(session, false);

        server.sendHeartbeat(0, 1);
        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN, MessageType.HEARTBEAT_REQUEST),
            session.getSentMessageTypes());
    }

    @Test
    void testRunSafely() {
        assertDoesNotThrow(() -> Server.runSafely(() -> {
            throw new RuntimeException("intended runtime exception");
        }));
    }

    private void simulateConnect(final FakeWsSession session) {
        server.handleConnect(new WsConnectContext(session.getId(), session));
    }

    private void simulateJoin(final FakeWsSession session) {
        simulateJoin(session, true);
    }

    private void simulateJoin(final FakeWsSession session, final boolean isReisen) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MutationType.PLAYER_JOIN.getIndex());
        byteArrayOutputStream.write(isReisen ? 1 : 0);
        simulateBinaryData(session, byteArrayOutputStream.toByteArray());
    }

    @SneakyThrows
    private void simulateBinaryData(final FakeWsSession session, final byte[] data) {
        server.handleBinaryMessage(new WsBinaryMessageContext(session.getId(), session, data, 0, data.length));
    }

    private void simulateClose(final FakeWsSession session) {
        server.handleClose(new WsCloseContext(session.getId(), session, 200, null));
    }
}
