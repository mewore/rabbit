package moe.mewore.rabbit.backend;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServerSettingsTest {

    @Test
    void testGetPort_unset() {
        assertEquals(8100, new ServerSettings(new String[0], Collections.emptyMap()).getPort());
    }

    @Test
    void testGetPort_argument() {
        assertEquals(8101, new ServerSettings(new String[]{"--rabbit.port=8102", "--rabbit.port=8101", "--unrelated"},
            Collections.emptyMap()).getPort());
    }

    @Test
    void testGetPort_environmentVariable() {
        assertEquals(8101, new ServerSettings(new String[0], Map.of("rabbit.port", "8101")).getPort());
    }

    @Test
    void testGetPort_invalid() {
        assertEquals(8100, new ServerSettings(new String[]{"--rabbit.port=asd"}, Collections.emptyMap()).getPort());
    }

    @Test
    void testGetExternalStaticLocation() {
        assertEquals("other/external", new ServerSettings(new String[]{"--rabbit.static.external=other/external"},
            Collections.emptyMap()).getExternalStaticLocation());
    }

    @Test
    void testGetExternalStaticLocation_unset() {
        assertNull(new ServerSettings(new String[]{}, Collections.emptyMap()).getExternalStaticLocation());
    }
}
