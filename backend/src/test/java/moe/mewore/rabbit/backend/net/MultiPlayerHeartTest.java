package moe.mewore.rabbit.backend.net;

import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.NonNull;
import moe.mewore.rabbit.backend.simulation.player.RabbitPlayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiPlayerHeartTest {

    @Mock
    private BiConsumer<@NonNull Integer, @NonNull Integer> sendFunction;

    private MultiPlayerHeart heart;

    @BeforeEach
    void setUp() {
        heart = new MultiPlayerHeart(3, sendFunction);
    }

    @Test
    void testDoStep_fiveTimes() {
        final RabbitPlayer firstPlayer = mock(RabbitPlayer.class);
        when(firstPlayer.getId()).thenReturn(0);
        final RabbitPlayer secondPlayer = mock(RabbitPlayer.class);
        when(secondPlayer.getId()).thenReturn(2);
        heart.addPlayer(firstPlayer);
        heart.addPlayer(secondPlayer);

        for (int i = 0; i < 5; i++) {
            heart.doStep();
        }

        final var targetIdCaptor = ArgumentCaptor.forClass(Integer.class);
        final var beatIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(sendFunction, times(3)).accept(targetIdCaptor.capture(), beatIdCaptor.capture());
        assertEquals(List.of(0, 2, 0), targetIdCaptor.getAllValues());
        assertEquals(List.of(1, 1, 2), beatIdCaptor.getAllValues());
    }

    @Test
    void testDoStep_50TimesWithOnePlayer() {
        final RabbitPlayer player = mock(RabbitPlayer.class);
        when(player.getId()).thenReturn(0);
        heart.addPlayer(player);

        for (int i = 0; i < 50; i++) {
            heart.doStep();
        }

        final var targetIdCaptor = ArgumentCaptor.forClass(Integer.class);
        final var beatIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(sendFunction, times(10)).accept(targetIdCaptor.capture(), beatIdCaptor.capture());
        assertEquals(List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), targetIdCaptor.getAllValues());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), beatIdCaptor.getAllValues());
    }

    @Test
    void testGetStepTimeInterval() {
        assertEquals(111, heart.getStepTimeInterval());
    }

    @Test
    void testRemovePlayer() {
        final RabbitPlayer player = mock(RabbitPlayer.class);
        when(player.getId()).thenReturn(0);
        heart.addPlayer(player);

        heart.removePlayer(player);
        for (int i = 0; i < 10; i++) {
            heart.doStep();
        }
        verify(sendFunction, never()).accept(any(), any());
    }

    @Test
    void testReceive() {
        final RabbitPlayer player = mock(RabbitPlayer.class);
        when(player.getId()).thenReturn(0);
        heart.addPlayer(player);
        heart.doStep();
        final var beatIdCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(sendFunction).accept(eq(0), beatIdCaptor.capture());

        final var beatId = beatIdCaptor.getValue();
        heart.receive(player, beatId);
        verify(player).setLatency(anyInt());
    }

    @Test
    void testReceive_noSuchPlayer() {
        final RabbitPlayer player = mock(RabbitPlayer.class);
        when(player.getId()).thenReturn(0);
        heart.receive(player, 0);
        verify(player, never()).setLatency(anyInt());
    }
}
