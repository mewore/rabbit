package moe.mewore.rabbit.backend.simulation.data;

import javax.vecmath.Vector3f;

import org.checkerframework.checker.nullness.qual.Nullable;

public class FrameSection {

    private final int from;

    private final int to;

    private byte @Nullable [] frame = null;

    private int index;

    FrameSection(final int from, final int length) {
        this.from = index = from;
        to = from + length;
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public byte readByte() {
        assert
            index < to && frame != null : "The index (" + index + ") should be less than the limit (" + to +
            ") and the frame should not be null (which is " + (frame != null) + ")";
        return frame[index++];
    }

    public int readUnsignedByte() {
        return readByte() & 0xff;
    }

    public short readShort() {
        return (short) readUnsignedShort();
    }

    public int readUnsignedShort() {
        return (readUnsignedByte() << 8) | readUnsignedByte();
    }

    public int readInt() {
        return (readUnsignedShort() << 16) | readUnsignedShort();
    }

    public long readLong() {
        return ((long) (readInt()) << 32L) | (long) (readInt());
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public Vector3f readIntoVector3f(final Vector3f target) {
        target.set(readFloat(), readFloat(), readFloat());
        return target;
    }

    public void writeBoolean(final boolean value) {
        writeByte(value ? 1 : 0);
    }

    public void writeByte(final int value) {
        assert
            index < to && frame != null : "The index (" + index + ") should be less than the limit (" + to +
            ") and the frame should not be null (which is " + (frame != null) + ")";
        frame[index++] = (byte) (value & 0xFF);
    }

    public void writeShort(final int value) {
        writeByte(value >>> 8);
        writeByte(value);
    }

    public void writeInt(final int value) {
        writeShort(value >>> 16);
        writeShort(value);
    }

    public void writeLong(final long value) {
        writeInt((int) (value >>> 32L));
        writeInt((int) (value & ((1L << 32L) - 1)));
    }

    public void writeFloat(final float value) {
        writeInt(Float.floatToIntBits(value));
    }

    public void writeDouble(final double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    public void writeVector3f(final Vector3f value) {
        writeFloat(value.x);
        writeFloat(value.y);
        writeFloat(value.z);
    }

    public void setFrame(final byte[] frame) {
        assert
            this.frame == null || frame.length == this.frame.length :
            "The new frame (with length " + frame.length + ") should be as long as the last frame (" +
                this.frame.length + ")";
        assert
            from >= 0 && to <= frame.length :
            "The section range [" + from + ", " + to + ") should not be anywhere out of the new frame range [0, " +
                frame.length + ")";
        this.frame = frame;
        index = from;
    }

    public boolean isAtEnd() {
        return index == to;
    }
}
