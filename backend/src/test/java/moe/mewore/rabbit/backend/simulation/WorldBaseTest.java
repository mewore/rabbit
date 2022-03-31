package moe.mewore.rabbit.backend.simulation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.Getter;
import moe.mewore.rabbit.backend.simulation.player.FakeInput;
import moe.mewore.rabbit.backend.simulation.player.ImmutableFakePlayer;
import moe.mewore.rabbit.backend.simulation.player.Player;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldBaseTest {

    @Mock
    private Player<PlayerInput> firstPlayer;

    @Mock
    private Player<PlayerInput> secondPlayer;

    @Captor
    private ArgumentCaptor<PlayerInput> inputCaptor;

    private TestWorld world;

    @BeforeEach
    void setUp() {
        world = new TestWorld(Map.of(0, firstPlayer, 1, secondPlayer));
    }

    @Test
    void testGetFrameId() {
        assertEquals(0L, world.getFrameId());
        world.doStep(1f);
        assertEquals(1L, world.getFrameId());
    }

    @Test
    void testGetMaxPlayerCount() {
        assertEquals(3, world.getMaxPlayerCount());
    }

    @Test
    void testReservePlayerIndex() {
        assertEquals(0, world.reservePlayerIndex());
        assertEquals(1, world.reservePlayerIndex());
        assertEquals(2, world.reservePlayerIndex());
        assertNull(world.reservePlayerIndex());
    }

    @Test
    void testReservePlayerIndex_withRemovals() {
        assertEquals(0, world.reservePlayerIndex());
        assertEquals(1, world.reservePlayerIndex());
        assertEquals(2, world.reservePlayerIndex());
        assertTrue(world.removePlayer(ImmutableFakePlayer.builder().index(0).build()));
        assertTrue(world.removePlayer(ImmutableFakePlayer.builder().index(2).build()));
        assertTrue(world.removePlayer(ImmutableFakePlayer.builder().index(1).build()));
        assertEquals(1, world.reservePlayerIndex());
        assertEquals(2, world.reservePlayerIndex());
        assertEquals(0, world.reservePlayerIndex());
    }

    @Test
    void testNextPlayerUid() {
        assertEquals(1, world.nextPlayerUid());
        assertEquals(2, world.nextPlayerUid());
    }

    @Test
    void testHasPlayers_withoutPlayers() {
        assertFalse(world.hasPlayers());
    }

    @Test
    void testHasPlayers_withReservedIndex() {
        world.reservePlayerIndex();
        assertTrue(world.hasPlayers());
    }

    @Test
    void testGetFrameSize() {
        assertEquals(8, world.getFrameSize());
    }

    @Test
    void testForEachPlayer() {
        final List<Player<?>> callArguments = new ArrayList<>(2);
        world.forEachPlayer(callArguments::add);
        assertEquals(2, callArguments.size());
        assertTrue(callArguments.contains(firstPlayer));
        assertTrue(callArguments.contains(secondPlayer));
    }

    @Test
    void testRemovePlayer() {
        final Integer index = world.reservePlayerIndex();
        assertNotNull(index);
        assertTrue(world.hasPlayers());
        assertEquals(0, world.getAfterPlayerRemovalCallCount());

        final var player = ImmutableFakePlayer.builder().index(index).build();
        world.removePlayer(player);
        assertFalse(world.hasPlayers());
        assertEquals(1, world.getAfterPlayerRemovalCallCount());
    }

    @Test
    void testRemove_nonExistent() {
        world.removePlayer(ImmutableFakePlayer.builder().index(0).build());
        assertEquals(0, world.getAfterPlayerRemovalCallCount());
    }

    @Test
    void testApplyInputs() {
        when(firstPlayer.getIndex()).thenReturn(0);
        when(secondPlayer.getIndex()).thenReturn(1);
        final var firstInput = new FakeInput(1, -12);
        final var secondInput = new FakeInput(2, -4);
        world.applyInputs(List.of(new ArrayDeque<>(List.of(firstInput, secondInput)), new ArrayDeque<>(List.of())),
            false);

        verify(firstPlayer, times(2)).applyInput(inputCaptor.capture());
        assertArrayEquals(new int[]{1, 2}, inputCaptor.getAllValues().stream().mapToInt(PlayerInput::getId).toArray());
        verify(secondPlayer, never()).applyInput(any());
    }

    @Test
    void testApplyInputs_doNotForce() {
        final AtomicInteger inputId = new AtomicInteger(2);
        when(firstPlayer.getInputId()).thenAnswer(invocation -> inputId.get());
        doAnswer(invocation -> inputId.getAndSet(((PlayerInput) (invocation.getArgument(0))).getId())).when(firstPlayer)
            .applyInput(any());
        when(firstPlayer.getIndex()).thenReturn(0);
        when(secondPlayer.getIndex()).thenReturn(1);

        world.applyInputs(
            List.of(new ArrayDeque<>(List.of(new FakeInput(1, -12), new FakeInput(4, -4), new FakeInput(3, -4))),
                new ArrayDeque<>(List.of())), false);

        verify(firstPlayer, atLeastOnce()).applyInput(inputCaptor.capture());
        assertEquals(List.of(4),
            inputCaptor.getAllValues().stream().map(PlayerInput::getId).collect(Collectors.toList()));
    }

    @Test
    void testApplyInputs_force() {
        final AtomicInteger inputId = new AtomicInteger(2);
        doAnswer(invocation -> inputId.getAndSet(((PlayerInput) (invocation.getArgument(0))).getId())).when(firstPlayer)
            .applyInput(any());
        when(firstPlayer.getIndex()).thenReturn(0);
        when(secondPlayer.getIndex()).thenReturn(1);

        world.applyInputs(
            List.of(new ArrayDeque<>(List.of(new FakeInput(1, -12), new FakeInput(4, -4), new FakeInput(3, -4))),
                new ArrayDeque<>(List.of())), true);

        verify(firstPlayer, atLeastOnce()).applyInput(inputCaptor.capture());
        assertEquals(List.of(1, 4, 3),
            inputCaptor.getAllValues().stream().map(PlayerInput::getId).collect(Collectors.toList()));
        verify(firstPlayer, never()).getInputId();
    }

    @Test
    void testApplyInputs_futureInputs() {
        when(firstPlayer.getIndex()).thenReturn(0);
        when(secondPlayer.getIndex()).thenReturn(1);
        final var firstInput = new FakeInput(1, -12);
        final var secondInput = new FakeInput(2, 4);
        final var thirdInput = new FakeInput(1, 30);
        world.applyInputs(
            List.of(new ArrayDeque<>(List.of(firstInput, secondInput)), new ArrayDeque<>(List.of(thirdInput))), false);

        verify(firstPlayer).applyInput(inputCaptor.capture());
        assertSame(firstInput, inputCaptor.getValue());
        verify(secondPlayer, never()).applyInput(any());
    }

    @Test
    void testGetPlayerAsMap() {
        when(secondPlayer.getIndex()).thenReturn(1);
        final Map<Integer, Player<PlayerInput>> players = world.getPlayersAsMap();
        assertNotSame(world.getPlayerMap(), players);
        assertEquals(world.getPlayerMap(), players);
    }

    @Test
    void testApplyInputs_unordered() {
        // Normally, inputs should be ordered in an increasing order of their frame ID, but let's assume that's not the
        // case here
        final var firstInput = new FakeInput(1, 5);
        final var secondInput = new FakeInput(2, -5);
        world.applyInputs(List.of(new ArrayDeque<>(List.of(firstInput, secondInput)), new ArrayDeque<>(List.of())),
            false);

        verify(firstPlayer, never()).applyInput(any());
        verify(secondPlayer, never()).applyInput(any());
    }

    @Test
    void testLoad() {
        final byte[] frame = new byte[world.getFrameSize()];
        frame[7] = 10;
        world.load(frame);
        assertEquals(10L, world.getFrameId());
        verify(firstPlayer).load(same(frame));
        verify(secondPlayer).load(same(frame));
    }

    @Test
    void testStore() {
        final byte[] frame = new byte[world.getFrameSize()];
        Arrays.fill(frame, (byte) 10);
        world.store(frame);
        assertEquals(0, frame[7]);
        verify(firstPlayer).store(same(frame));
        verify(secondPlayer).store(same(frame));
    }

    private static class TestWorld extends WorldBase<PlayerInput, Player<PlayerInput>> {

        @Getter
        private final Map<Integer, Player<PlayerInput>> playerMap;

        @Getter
        private int afterPlayerRemovalCallCount = 0;

        public TestWorld(final Map<Integer, Player<PlayerInput>> playerMap) {
            super(3);
            this.playerMap = playerMap;
            afterPlayerRemoval = player -> ++afterPlayerRemovalCallCount;
        }

        @Override
        public void forEachPlayer(final Consumer<Player<PlayerInput>> playerConsumer) {
            playerMap.values().forEach(playerConsumer);
        }

        @Override
        public void doStep(final float deltaSeconds) {
            ++frameId;
        }
    }
}
