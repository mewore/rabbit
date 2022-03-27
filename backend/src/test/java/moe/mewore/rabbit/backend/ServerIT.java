package moe.mewore.rabbit.backend;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(settings.getExternalStaticLocation()).thenReturn(".");

        final Server server = Server.create(settings).start();
        try {
            final HttpURLConnection testHttp = (HttpURLConnection) new URL(
                String.format("http://localhost:%d/test.txt", port)).openConnection();
            assertEquals(200, testHttp.getResponseCode());
            assertEquals("Test\n", new String(testHttp.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

            final HttpURLConnection editorsHttp = (HttpURLConnection) new URL(
                String.format("http://localhost:%d/editors", port)).openConnection();
            assertEquals(200, editorsHttp.getResponseCode());
            assertEquals("[]", new String(editorsHttp.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            server.stop();
        }
    }

    @Test
    void testInitialize_noExternalStaticLocation() throws IOException, InterruptedException {
        final ServerSettings settings = mock(ServerSettings.class);
        final int port = MIN_PORT + new SecureRandom().nextInt(PORT_RANGE);
        when(settings.getPort()).thenReturn(port);

        final Server server = Server.create(settings).start();
        try {
            final HttpURLConnection editorsHttp = (HttpURLConnection) new URL(
                String.format("http://localhost:%d/editors", port)).openConnection();
            assertEquals(200, editorsHttp.getResponseCode());
            assertEquals("[]", new String(editorsHttp.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            server.stop();
        }
    }
}
