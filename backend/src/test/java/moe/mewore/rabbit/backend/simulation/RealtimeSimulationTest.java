package moe.mewore.rabbit.backend.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import moe.mewore.rabbit.backend.simulation.player.FakeInput;
import moe.mewore.rabbit.backend.simulation.player.ImmutableFakePlayer;
import moe.mewore.rabbit.backend.simulation.player.Player;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeSimulationTest {

    private static final Player<? extends PlayerInput> DEFAULT_PLAYER = new ImmutableFakePlayer();

    @Mock
    private World<PlayerInput, Player<PlayerInput>> world;

    @Captor
    private ArgumentCaptor<List<Queue<PlayerInput>>> appliedInputCaptor;

    private static void verifyLastAppliedInputFrames(final RealtimeSimulation<PlayerInput> simulation,
        final long... frameIds) {
        final List<PlayerInputEvent<PlayerInput>> lastAppliedInputs = simulation.getLastAppliedInputs();
        assertNotNull(lastAppliedInputs);
        assertArrayEquals(frameIds, lastAppliedInputs.stream().mapToLong(PlayerInputEvent::getFrameId).toArray());
    }

    @BeforeEach
    void setUp() {
        when(world.getMaxPlayerCount()).thenReturn(1);
        when(world.getFrameSize()).thenReturn(1);
    }

    private World<PlayerInput, Player<PlayerInput>> prepareWorld(final AtomicLong frameId) {
        Mockito.doAnswer(invocation -> frameId.incrementAndGet()).when(world).doStep(anyFloat());
        Mockito.doAnswer(invocation -> {
            final byte[] frame = invocation.getArgument(0);
            assert frameId.get() < 100;
            frame[0] = (byte) frameId.get();
            return null;
        }).when(world).store(any());
        when(world.getFrameId()).thenAnswer(invocation -> frameId.get());
        return world;
    }

    private World<PlayerInput, Player<PlayerInput>> prepareWorld() {
        return prepareWorld(new AtomicLong(0L));
    }

    @Test
    void testGetCurrentSnapshot() {
        final AtomicLong frameId = new AtomicLong(0);
        final var simulation = new RealtimeSimulation<>(prepareWorld(frameId));
        simulation.update(System.currentTimeMillis() + 1000L);
        assertNotEquals(0L, frameId.get());
        final byte[] frame = simulation.getCurrentSnapshot();
        assertEquals(frameId.get(), frame[0]);
    }

    @Test
    void testGetPastSnapshot() {
        final AtomicLong frameId = new AtomicLong(0);
        final var simulation = new RealtimeSimulation<>(prepareWorld(frameId));
        simulation.update(System.currentTimeMillis() + 1000L);
        assertNotEquals(0L, frameId.get());
        final byte[] frame = simulation.getPastSnapshot(500);
        final long pastSnapshotFrame = frame[0];
        assertTrue(pastSnapshotFrame > 0, "The frame ~500ms after the beginning should be with an ID greater than 0");
        assertTrue(pastSnapshotFrame < frameId.get(),
            "The frame ~500ms after the beginning should be with an ID less than the latest Frame");
    }

    @Test
    void testAcceptInput() throws InterruptedException {
        final var input = new FakeInput(0, 0L);
        final var simulation = new RealtimeSimulation<>(world);

        simulation.acceptInput(DEFAULT_PLAYER, input);
        verify(world, never()).applyInputs(any(), eq(false));
        assertNull(simulation.getLastAppliedInputs());
    }

    @Test
    void testAcceptInput_withUpdate() throws InterruptedException {
        final var input = new FakeInput(0, 1L);
        final var simulation = new RealtimeSimulation<>(prepareWorld());
        simulation.acceptInput(DEFAULT_PLAYER, input);

        simulation.advanceToFrame(0L);
        verify(world, never()).applyInputs(any(), eq(false));

        verifyLastAppliedInputFrames(simulation, 1L);
        final List<PlayerInputEvent<PlayerInput>> lastAppliedInputs = simulation.getLastAppliedInputs();
        assertNotNull(lastAppliedInputs);
        assertSame(input, simulation.getLastAppliedInputs().get(0).getInput());

        simulation.advanceToFrame(1L);
        verify(world).applyInputs(appliedInputCaptor.capture(), eq(false));
        assertEquals(List.of(input), new ArrayList<>(appliedInputCaptor.getValue().get(0)));
        assertNull(simulation.getLastAppliedInputs());
    }

    @Test
    void testAcceptInput_withUpdate_unreasonableFrame() throws InterruptedException {
        final var input = new FakeInput(0, -1000L);
        final var simulation = new RealtimeSimulation<>(prepareWorld());
        simulation.acceptInput(DEFAULT_PLAYER, input);

        simulation.advanceToFrame(0L);
        verify(world, never()).applyInputs(any(), eq(false));
        verifyLastAppliedInputFrames(simulation, 0L);

        simulation.advanceToFrame(1L);
        verify(world).applyInputs(appliedInputCaptor.capture(), eq(false));
        assertEquals(List.of(input), new ArrayList<>(appliedInputCaptor.getValue().get(0)));
        assertNull(simulation.getLastAppliedInputs());
    }

    @Test
    void testAcceptInput_withUpdate_inTheFarFuture() throws InterruptedException {
        when(world.getFrameId()).thenReturn(0L);
        final var simulation = new RealtimeSimulation<>(world);
        simulation.acceptInput(DEFAULT_PLAYER, new FakeInput(0, 100000000L));
        simulation.acceptInput(DEFAULT_PLAYER, new FakeInput(0, 1000000000000000000L));

        simulation.advanceToFrame(0L);
        verifyLastAppliedInputFrames(simulation, 20L, 20L);
    }

    @Test
    void testAdvanceToFrame() {
        final var simulation = new RealtimeSimulation<>(prepareWorld());
        assertEquals(0L, world.getFrameId());
        simulation.advanceToFrame(1L);
        assertEquals(1L, world.getFrameId());
        assertNull(simulation.getLastAppliedInputs());
        verify(world).doStep(0.016666668f);
    }

    @Test
    void testAdvanceToFrame_withPastInputs() throws InterruptedException {
        final AtomicLong frameId = new AtomicLong();
        final var simulation = new RealtimeSimulation<>(prepareWorld(frameId));
        simulation.advanceToFrame(3L);
        assertEquals(3L, world.getFrameId());
        verify(world, times(3)).applyInputs(any(), eq(false));
        verify(world, never()).applyInputs(any(), eq(true));

        Mockito.doAnswer(invocation -> frameId.getAndSet(((byte[]) invocation.getArgument(0))[0]))
            .when(world)
            .load(any());
        final var input = new FakeInput(0, 1L);
        simulation.acceptInput(DEFAULT_PLAYER, input);
        simulation.advanceToFrame(5L);
        assertEquals(5L, world.getFrameId());
        verify(world, times(7)).applyInputs(any(), eq(false));
        verify(world).applyInputs(any(), eq(true));
    }

    @Test
    void testUpdate() {
        final var simulation = new RealtimeSimulation<>(prepareWorld());
        simulation.update(System.currentTimeMillis() + 100);
        assertTrue(world.getFrameId() > 0);
        verify(world, atLeastOnce()).doStep(0.016666668f);
    }
}
