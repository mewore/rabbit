package moe.mewore.rabbit.data;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link DataOutput} which does not throw any {@link IOException}s.
 */
public interface SafeDataOutput extends DataOutput {

    @Override
    void write(int b);

    @Override
    void write(byte @NonNull [] b);

    @Override
    void write(byte @NonNull [] b, int off, int len);

    @Override
    void writeBoolean(boolean v);

    void writeNullableBoolean(@Nullable Boolean v);

    @Override
    void writeByte(int v);

    @Override
    void writeShort(int v);

    @Override
    void writeChar(int v);

    @Override
    void writeInt(int v);

    @Override
    void writeLong(long v);

    @Override
    void writeFloat(float v);

    @Override
    void writeDouble(double v);

    @Override
    void writeBytes(@NonNull String s);

    @Override
    void writeChars(@NonNull String s);

    @Override
    void writeUTF(@NonNull String s);

    void writeAsciiWithLength(@NonNull String s);

    <T extends BinaryEntity> void writeArray(@NonNull T @NonNull [] entities);

    <@NonNull T extends BinaryEntity> void writeCollection(@NonNull Collection<T> entities);

    <@NonNull K, @NonNull V> void writeMap(@NonNull Map<K, V> entityMap, @NonNull Consumer<K> keyWriter,
        @NonNull Consumer<V> valueWriter);
}
