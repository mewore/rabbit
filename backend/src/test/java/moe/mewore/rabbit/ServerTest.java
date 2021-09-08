package moe.mewore.rabbit;

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
import moe.mewore.rabbit.data.ByteArrayDataOutput;
import moe.mewore.rabbit.entities.messages.MessageType;
import moe.mewore.rabbit.entities.mutations.MutationType;
import moe.mewore.rabbit.entities.world.FakeForest;
import moe.mewore.rabbit.mock.ws.FakeWsSession;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ServerTest {

    private Server server;

    private static double[] readNineDoubles(final DataInput eventInput) throws IOException {
        final double[] result = new double[9];
        for (int i = 0; i < 9; i++) {
            result[i] = eventInput.readDouble();
        }
        return result;
    }

    private static void writeDoubles(final ByteArrayOutputStream byteArrayOutputStream, final double... values) {
        final ByteArrayDataOutput dataOutput = new ByteArrayDataOutput(byteArrayOutputStream);
        for (final double value : values) {
            dataOutput.writeDouble(value);
        }
    }

    @BeforeEach
    void setUp() {
        server = new Server(new ServerSettings(new String[0], Map.of()), mock(Javalin.class), new FakeForest());
    }

    @Test
    void testHandleConnect() {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        assertEquals(List.of(MessageType.FOREST_DATA), session.getSentMessageTypes());
    }

    @Test
    void testHandleConnect_afterJoin() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        simulateJoin(session);

        final FakeWsSession otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        assertEquals(List.of(MessageType.FOREST_DATA, MessageType.JOIN), otherSession.getSentMessageTypes());
    }

    @Test
    void testHandleBinaryMessage_join() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);

        final FakeWsSession otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        simulateJoin(session, false);

        assertEquals(List.of(MessageType.FOREST_DATA, MessageType.JOIN), otherSession.getSentMessageTypes());
        final DataInput eventInput = new DataInputStream(new ByteArrayInputStream(otherSession.getSentData().get(1)));
        assertEquals(MessageType.JOIN.getIndex(), eventInput.readByte());
        assertEquals(1, eventInput.readInt());
        assertEquals(8, eventInput.readInt());
        final byte[] stringBytes = new byte[8];
        eventInput.readFully(stringBytes);
        assertEquals("Player 1", new String(stringBytes, StandardCharsets.US_ASCII));
        assertFalse(eventInput.readBoolean());
        assertArrayEquals(new double[]{0, 0, 0, 0, 0, 0, 0, 0, 0}, readNineDoubles(eventInput));
    }

    @Test
    void testHandleBinaryMessage_update() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        simulateJoin(session);

        final FakeWsSession otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(MutationType.UPDATE.getIndex());
        writeDoubles(byteArrayOutputStream, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        simulateBinaryData(session, byteArrayOutputStream.toByteArray());

        assertEquals(List.of(MessageType.FOREST_DATA, MessageType.JOIN, MessageType.UPDATE),
            otherSession.getSentMessageTypes());
        final DataInput eventInput = new DataInputStream(new ByteArrayInputStream(otherSession.getSentData().get(2)));
        assertEquals(MessageType.UPDATE.getIndex(), eventInput.readByte());
        assertEquals(1, eventInput.readInt());
        assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, readNineDoubles(eventInput));
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
            () -> simulateBinaryData(session, new byte[]{MutationType.JOIN.getIndex()}));
        assertEquals("There is already a player for session session! Cannot join again.", exception.getMessage());
    }

    @Test
    void testHandleBinaryMessage_update_notConnected() {
        final FakeWsSession session = new FakeWsSession("session");
        final Exception exception = assertThrows(IllegalArgumentException.class,
            () -> simulateBinaryData(session, new byte[]{MutationType.UPDATE.getIndex()}));
        assertEquals("There is no player for session session", exception.getMessage());
    }

    @Test
    void testHandleClose() {
        final FakeWsSession firstSession = new FakeWsSession("session");
        simulateConnect(firstSession);
        simulateClose(firstSession);
        assertEquals(List.of(MessageType.FOREST_DATA), firstSession.getSentMessageTypes());
    }

    @Test
    void testHandleClose_two() throws IOException {
        final FakeWsSession session = new FakeWsSession("session");
        simulateConnect(session);
        simulateJoin(session);

        final FakeWsSession otherSession = new FakeWsSession("other");
        simulateConnect(otherSession);
        simulateClose(session);

        assertEquals(List.of(MessageType.FOREST_DATA, MessageType.JOIN, MessageType.DISCONNECT),
            otherSession.getSentMessageTypes());
        final DataInput eventInput = new DataInputStream(new ByteArrayInputStream(otherSession.getSentData().get(2)));
        assertEquals(MessageType.DISCONNECT.getIndex(), eventInput.readByte());
        assertEquals(1, eventInput.readInt());
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
        byteArrayOutputStream.write(MutationType.JOIN.getIndex());
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
