package moe.mewore.rabbit.backend;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

import io.javalin.Javalin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerIT {

    @Test
    void testInitialize() throws IOException {
        final ServerSettings settings = mock(ServerSettings.class);
        final int port = 63200 + new SecureRandom().nextInt(100);
        when(settings.getPort()).thenReturn(port);

        final Javalin server = Server.start(settings);
        try {
            final HttpURLConnection http = (HttpURLConnection) new URL(
                String.format("http://localhost:%d/test.txt", port)).openConnection();
            assertEquals(200, http.getResponseCode());
            assertEquals("Test\n", new String(http.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            server.stop();
        }
    }
}
