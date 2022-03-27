package moe.mewore.rabbit.backend.net;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HeartTest {

    @Mock
    private Consumer<Integer> latencyConsumer;

    private Heart heart;

    @BeforeEach
    void setUp() {
        heart = new Heart(latencyConsumer);
    }

    @Test
    void testPrepareBeat() {
        assertEquals(1, heart.prepareBeat());
    }

    @Test
    void testPrepareBeat_filled() {
        for (int i = 1; i <= 10; i++) {
            assertEquals(i, heart.prepareBeat());
        }
        assertNull(heart.prepareBeat());
    }

    @Test
    void testReceive() {
        heart.prepareBeat();
        final Integer second = heart.prepareBeat();
        assertNotNull(second);
        heart.receive(second);
        final var integerCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(latencyConsumer).accept(integerCaptor.capture());
        assertTrue(integerCaptor.getValue() > 0);
    }

    @Test
    void testReceive_invalidId() {
        heart.receive(-1);
        verify(latencyConsumer, never()).accept(any());
    }
}
