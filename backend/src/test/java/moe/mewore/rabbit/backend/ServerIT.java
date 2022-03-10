package moe.mewore.rabbit.backend;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerIT {

    private static final int MIN_PORT = 63200;

    private static final int PORT_RANGE = 100;

    @Test
    void testInitialize() throws IOException, InterruptedException {
        final ServerSettings settings = mock(ServerSettings.class);
        final int port = MIN_PORT + new SecureRandom().nextInt(PORT_RANGE);
        when(settings.getPort()).thenReturn(port);

        final Server server = Server.create(settings).start();
        try {
            final HttpURLConnection http = (HttpURLConnection) new URL(
                String.format("http://localhost:%d/test.txt", port)).openConnection();
            assertEquals(200, http.getResponseCode());
            assertEquals("Test\n", new String(http.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            server.stop();
        }
    }

    @Test
    void testStartWhileStarted() throws IOException, InterruptedException {
        final ServerSettings settings = mock(ServerSettings.class);
        final int port = MIN_PORT + new SecureRandom().nextInt(PORT_RANGE);
        when(settings.getPort()).thenReturn(port);

        final Server server = Server.create(settings).start();
        try {
            final var exception = assertThrows(IllegalStateException.class, server::start);
            assertEquals("The server expected to transition from state STOPPED to STARTING but its state was RUNNING",
                exception.getMessage());
        } finally {
            server.stop();
        }
    }

    @Test
    void testStopWhileStopped() throws IOException {
        final ServerSettings settings = mock(ServerSettings.class);
        when(settings.getPort()).thenReturn(MIN_PORT);

        final Server server = Server.create(settings);
        final var exception = assertThrows(IllegalStateException.class, server::stop);
        assertEquals("The server expected to transition from state RUNNING to STOPPING but its state was STOPPED",
            exception.getMessage());
    }
}
