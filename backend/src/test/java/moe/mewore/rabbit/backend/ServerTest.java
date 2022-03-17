package moe.mewore.rabbit.backend;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;
import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import moe.mewore.rabbit.backend.messages.MessageType;
import moe.mewore.rabbit.backend.mock.FakeMap;
import moe.mewore.rabbit.backend.mock.ws.FakeWsSession;
import moe.mewore.rabbit.backend.mutations.MutationType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ServerTest {

    private Server server;

    @BeforeEach
    void setUp() {
        server = new Server(new ServerSettings(new String[0], Map.of()), mock(Javalin.class), new FakeMap());
    }

    @Test
    void testHandleConnect() {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        assertEquals(List.of(MessageType.MAP_DATA), session.getSentMessageTypes());
    }

    @Test
    void testHandleConnect_afterJoin() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        simulateJoin(session);

        final FakeWsSession otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN), otherSession.getSentMessageTypes());
    }

    @Test
    void testHandleBinaryMessage_join() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);

        final FakeWsSession otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        simulateJoin(session, false);

        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN), session.getSentMessageTypes());
        final DataInput eventInput = new DataInputStream(new ByteArrayInputStream(session.getSentData().get(1)));
        assertEquals(MessageType.JOIN.getIndex(), eventInput.readByte());
        assertEquals(0, eventInput.readInt()); // playerId
        assertEquals(8, eventInput.readInt()); // username length
        final byte[] stringBytes = new byte[8];
        eventInput.readFully(stringBytes);
        assertEquals("Player 1", new String(stringBytes, StandardCharsets.US_ASCII));
        assertFalse(eventInput.readBoolean()); // isReisen
        assertTrue(eventInput.readBoolean()); // isSelf

        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN), otherSession.getSentMessageTypes());
        final DataInput otherEventInput = new DataInputStream(
            new ByteArrayInputStream(otherSession.getSentData().get(1)));
        assertEquals(MessageType.JOIN.getIndex(), otherEventInput.readByte());
        assertEquals(0, otherEventInput.readInt()); // playerId
        assertEquals(8, otherEventInput.readInt()); // username length
        otherEventInput.readFully(stringBytes);
        assertEquals("Player 1", new String(stringBytes, StandardCharsets.US_ASCII));
        assertFalse(otherEventInput.readBoolean()); // isReisen
        assertFalse(otherEventInput.readBoolean()); // isSelf
    }

    @Test
    void testHandleBinaryMessage_playerInput() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        simulateJoin(session);

        final FakeWsSession otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MutationType.PLAYER_INPUT.getIndex());
        byteArrayOutputStream.writeBytes(new byte[3 * 4 + 4]);
        simulateBinaryData(session, byteArrayOutputStream.toByteArray());

        assertEquals(List.of(MessageType.MAP_DATA, MessageType.JOIN), otherSession.getSentMessageTypes());
        // The new state is not sent immediately; instead, it is sent at every world step
    }

    @Test
    void testHandleBinaryMessage_invalidMutationType() {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> simulateBinaryData(session, new byte[]{25}));
        assertEquals("There is no mutation type with index 25", exception.getMessage());
    }

    @Test
    void testHandleBinaryMessage_join_alreadyConnected() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateJoin(session);
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> simulateBinaryData(session, new byte[]{MutationType.PLAYER_JOIN.getIndex()}));
        assertEquals("There is already a player for session session! Cannot join again.", exception.getMessage());
    }

    @Test
    void testHandleBinaryMessage_update_notConnected() {
        final FakeWsSession session = new FakeWsSession("session");
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> simulateBinaryData(session, new byte[]{MutationType.PLAYER_INPUT.getIndex()}));
        assertEquals("There is no player for session session", exception.getMessage());
    }

    @Test
    void testHandleClose() {
        final FakeWsSession firstSession = new FakeWsSession("session");
        simulateConnect(firstSession);
        simulateClose(firstSession);
        assertEquals(List.of(MessageType.MAP_DATA), firstSession.getSentMessageTypes());
    }

    @Test
    void testHandleClose_two() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        simulateJoin(session);

        final FakeWsSession otherSession = new FakeWsSession("other");
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
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        simulateClose(session);
        simulateClose(session);
    }

    private void simulateConnect(final FakeWsSession session) {
        server.handleConnect(new WsConnectContext(session.getId(), session));
    }

    private void simulateJoin(final FakeWsSession session) throws IOException {
        simulateJoin(session, true);
    }

    private void simulateJoin(final FakeWsSession session, final boolean isReisen) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MutationType.PLAYER_JOIN.getIndex());
        byteArrayOutputStream.write(isReisen ? 1 : 0);
        simulateBinaryData(session, byteArrayOutputStream.toByteArray());
    }

    private void simulateBinaryData(final FakeWsSession session, final byte[] data) throws IOException {
        server.handleBinaryMessage(new WsBinaryMessageContext(session.getId(), session, data, 0, data.length));
    }

    private void simulateClose(final FakeWsSession session) {
        server.handleClose(new WsCloseContext(session.getId(), session, 200, null));
    }
}
