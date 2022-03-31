package moe.mewore.rabbit.backend.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.Synchronized;
import moe.mewore.rabbit.backend.simulation.data.FrameCompiler;
import moe.mewore.rabbit.backend.simulation.data.FrameDataType;
import moe.mewore.rabbit.backend.simulation.data.FrameSection;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializableEntity;
import moe.mewore.rabbit.backend.simulation.player.Player;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;

import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.LONG;

public abstract class WorldBase<I extends PlayerInput, P extends Player<I>> implements World<I, P> {

    protected final FrameCompiler frameCompiler = new FrameCompiler();

    @Getter
    protected final FrameSection headerFrameSection = frameCompiler.reserve(getHeaderFrameDataTypes());

    private final AtomicInteger playerUid = new AtomicInteger();

    @Getter
    private final int maxPlayerCount;

    private final boolean[] hasPlayerAt;

    private final Stack<Integer> freePlayerIndices;

    @Getter
    protected long frameId = 0;

    protected @Nullable Consumer<P> afterPlayerRemoval = null;

    private int playerCount = 0;

    public WorldBase(final int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
        hasPlayerAt = new boolean[maxPlayerCount];
        freePlayerIndices = new Stack<>();
        for (int i = maxPlayerCount - 1; i >= 0; i--) {
            freePlayerIndices.add(i);
        }
    }

    @Override
    public boolean hasPlayers() {
        return playerCount > 0;
    }

    @Override
    public int getFrameSize() {
        return frameCompiler.getSize();
    }

    @Override
    public Map<Integer, P> getPlayersAsMap() {
        final Map<Integer, P> result = new HashMap<>(playerCount);
        forEachPlayer(player -> result.put(player.getIndex(), player));
        return result;
    }

    /**
     * @param player The player to remove
     * @return Whether the player was in this world in the first place.
     */
    @Synchronized
    @Override
    public boolean removePlayer(final P player) {
        if (!hasPlayerAt[player.getIndex()]) {
            System.out.printf("There already is no player at index %d!%n", player.getIndex());
            return false;
        }
        freePlayerIndices.add(player.getIndex());
        hasPlayerAt[player.getIndex()] = false;
        --playerCount;
        if (afterPlayerRemoval != null) {
            afterPlayerRemoval.accept(player);
        }
        return true;
    }

    @Override
    public void applyInputs(final List<Queue<I>> pendingInputsByPlayerIndex, final boolean force) {
        forEachPlayer(player -> {
            final Queue<I> inputs = pendingInputsByPlayerIndex.get(player.getIndex());
            I candidate = inputs.peek();
            while (candidate != null && (frameId >= candidate.getFrameId())) {
                if (force || candidate.getId() >= player.getInputId()) {
                    player.applyInput(candidate);
                }
                inputs.remove();
                candidate = inputs.peek();
            }
        });
    }

    @Override
    public void load(final byte[] frame) {
        headerFrameSection.setFrame(frame);
        frameId = headerFrameSection.readLong();
        forEachSerializableEntity(entity -> entity.load(frame));
    }

    @Override
    public void store(final byte[] frame) {
        headerFrameSection.setFrame(frame);
        headerFrameSection.writeLong(frameId);
        forEachSerializableEntity(entity -> entity.store(frame));
    }

    protected FrameDataType[] getHeaderFrameDataTypes() {
        return new FrameDataType[]{LONG};
    }

    /**
     * Find a free slot for a player and reserve it.
     *
     * @return The reserved index.
     */
    @Synchronized
    protected @Nullable Integer reservePlayerIndex() {
        if (freePlayerIndices.isEmpty()) {
            return null;
        }
        final int index = freePlayerIndices.pop();
        hasPlayerAt[index] = true;
        ++playerCount;
        return index;
    }

    protected int nextPlayerUid() {
        return playerUid.incrementAndGet();
    }

    protected void forEachSerializableEntity(final Consumer<FrameSerializableEntity> consumer) {
        forEachPlayer(consumer::accept);
    }
}
