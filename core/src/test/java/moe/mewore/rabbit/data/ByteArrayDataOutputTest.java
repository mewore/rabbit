package moe.mewore.rabbit.data;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.RequiredArgsConstructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ByteArrayDataOutputTest {

    private ByteArrayOutputStream byteArrayOutputStream;

    private ByteArrayDataOutput dataOutput;

    @BeforeEach
    void setUp() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        dataOutput = new ByteArrayDataOutput(byteArrayOutputStream);
    }

    @Test
    void testWriteSingle() {
        dataOutput.write(0xFF42);
        assertArrayEquals(new byte[]{0x42}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteArray() {
        dataOutput.write(new byte[]{0x12, 0x34});
        assertArrayEquals(new byte[]{0x12, 0x34}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteArrayWithOffset() {
        dataOutput.write(new byte[]{0x12, 0x34}, 1, 1);
        assertArrayEquals(new byte[]{0x34}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteBoolean_true() {
        dataOutput.writeBoolean(true);
        assertArrayEquals(new byte[]{1}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteBoolean_false() {
        dataOutput.writeBoolean(false);
        assertArrayEquals(new byte[]{0}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteNullableBoolean_true() {
        dataOutput.writeNullableBoolean(true);
        assertArrayEquals(new byte[]{1}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteNullableBoolean_false() {
        dataOutput.writeNullableBoolean(false);
        assertArrayEquals(new byte[]{0}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteNullableBoolean_null() {
        dataOutput.writeNullableBoolean(null);
        assertArrayEquals(new byte[]{-1}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteByte() {
        dataOutput.writeByte(0xFF42);
        assertArrayEquals(new byte[]{0x42}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteShort() {
        dataOutput.writeShort(0xFFFF1234);
        assertArrayEquals(new byte[]{0x12, 0x34}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteChar() {
        dataOutput.writeChar(0xFFFF1234);
        assertArrayEquals(new byte[]{0x12, 0x34}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteInt() {
        dataOutput.writeInt(0x12345678);
        assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteLong() {
        dataOutput.writeLong(0x1234567812345678L);
        assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78, 0x12, 0x34, 0x56, 0x78},
            byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteFloat() {
        dataOutput.writeFloat(Float.intBitsToFloat(0xFF34));
        assertArrayEquals(new byte[]{0, 0, (byte) 0xFF, 0x34}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteDouble() {
        dataOutput.writeDouble(Double.longBitsToDouble(0x1234567812345678L));
        assertArrayEquals(new byte[]{0x12, 0x34, 0x56, 0x78, 0x12, 0x34, 0x56, 0x78},
            byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteBytes() {
        dataOutput.writeBytes(new String(new byte[]{0x1, 0x2}, StandardCharsets.US_ASCII));
        assertArrayEquals(new byte[]{0x1, 0x2}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteChars() {
        dataOutput.writeChars(new String(new byte[]{0x1, 0x2, 0x3, 0x4}, StandardCharsets.US_ASCII));
        assertArrayEquals(new byte[]{0, 0x1, 0, 0x2, 0, 0x3, 0, 0x4}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteUTF() {
        dataOutput.writeUTF(new String(new byte[]{0x1, 0x2, 0x3, 0x4}, StandardCharsets.UTF_8));
        assertArrayEquals(new byte[]{0x1, 0x2, 0x3, 0x4}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteAsciiWithLength() {
        dataOutput.writeAsciiWithLength(new String(new byte[]{0x1, 0x2, 0x3, 0x4}, StandardCharsets.US_ASCII));
        assertArrayEquals(new byte[]{0, 0, 0, 4, 0x1, 0x2, 0x3, 0x4}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteEntityArray() {
        dataOutput.writeArray(new BinaryEntity[]{new DumbEntity(0x12), new DumbEntity(0x34)});
        assertArrayEquals(new byte[]{0, 0, 0, 2, 0x12, 0x34}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteCollection() {
        dataOutput.writeCollection(List.of(new DumbEntity(0x12), new DumbEntity(0x34)));
        assertArrayEquals(new byte[]{0, 0, 0, 2, 0x12, 0x34}, byteArrayOutputStream.toByteArray());
    }

    @Test
    void testWriteMap() {
        dataOutput.writeMap(Map.of(1, 0x12), dataOutput::writeInt, dataOutput::writeByte);
        assertArrayEquals(new byte[]{0, 0, 0, 1, 0, 0, 0, 1, 0x12}, byteArrayOutputStream.toByteArray());
    }

    @RequiredArgsConstructor
    private static class DumbEntity extends BinaryEntity {

        private final int value;

        @Override
        public void appendToBinaryOutput(final SafeDataOutput output) {
            output.writeByte(value);
        }
    }
}
