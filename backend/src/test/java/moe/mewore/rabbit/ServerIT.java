package moe.mewore.rabbit;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

import spark.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static spark.Service.ignite;

class ServerIT {

    @Test
    void testInitialize() throws IOException {
        final ServerSettings settings = mock(ServerSettings.class);
        final int port = 63200 + new SecureRandom().nextInt(100);
        when(settings.getPort()).thenReturn(port);

        final Service sparkService = ignite();
        final Server server = new Server(settings, sparkService);

        server.initialize();
        sparkService.awaitInitialization();
        final HttpURLConnection http = (HttpURLConnection) new URL(
            String.format("http://localhost:%d/test.txt", port)).openConnection();
        assertEquals(200, http.getResponseCode());
        assertEquals("Test\n", new String(http.getInputStream().readAllBytes(), StandardCharsets.UTF_8));

        sparkService.stop();
    }
}
