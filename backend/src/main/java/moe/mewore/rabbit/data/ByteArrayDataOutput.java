package moe.mewore.rabbit.data;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ByteArrayDataOutput implements SafeDataOutput {

    private final ByteArrayOutputStream outputStream;

    @Override
    public void write(final int b) {
        writeByte(b);
    }

    @Override
    public void write(final byte @NonNull [] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte @NonNull [] b, final int off, final int len) {
        outputStream.write(b, off, len);
    }

    @Override
    public void writeBoolean(final boolean v) {
        outputStream.write(v ? 1 : 0);
    }

    @Override
    public void writeNullableBoolean(final @Nullable Boolean v) {
        outputStream.write(v == null ? -1 : (v ? 1 : 0));
    }

    @Override
    public void writeByte(final int v) {
        outputStream.write(v);
    }

    @Override
    public void writeShort(final int v) {
        outputStream.write((v >>> 8) & 0xFF);
        outputStream.write((v) & 0xFF);
    }

    @Override
    public void writeChar(final int v) {
        writeShort(v);
    }

    @Override
    public void writeInt(final int v) {
        writeShort(v >>> 16);
        writeShort(v);
    }

    @Override
    public void writeLong(final long v) {
        writeInt((int) (v >>> 32));
        writeInt((int) v);
    }

    @Override
    public void writeFloat(final float v) {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(final double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(final @NonNull String s) {
        write(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void writeChars(final @NonNull String s) {
        for (int i = 0; i < s.length(); i++) {
            writeChar(s.charAt(i));
        }
    }

    @Override
    public void writeUTF(final @NonNull String s) {
        write(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void writeAsciiWithLength(final @NonNull String s) {
        writeBytesWithLength(s.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeBytesWithLength(final byte @NonNull [] bytes) {
        writeInt(bytes.length);
        write(bytes);
    }
}
