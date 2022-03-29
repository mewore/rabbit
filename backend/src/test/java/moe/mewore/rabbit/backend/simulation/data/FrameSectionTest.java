package moe.mewore.rabbit.backend.simulation.data;

import javax.vecmath.Vector3f;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameSectionTest {

    private byte[] frame;

    private FrameSection makeSection(final int... data) {
        frame = new byte[data.length];
        for (int i = 0; i < frame.length; i++) {
            assert (int) ((byte) data[i]) == data[i] : data[i] + " should fit into a byte";
            frame[i] = (byte) data[i];
        }
        final var frameSection = new FrameSection(0, frame.length);
        frameSection.setFrame(frame);
        return frameSection;
    }

    private FrameSection makeSectionWithLength(final int length) {
        final int[] data = new int[length];
        Arrays.fill(data, -5);
        return makeSection(data);
    }

    @Test
    void testBoolean_false() {
        final FrameSection section = makeSection(1);
        section.writeBoolean(false);
        assertTrue(section.isAtEnd());

        section.setFrame(frame);
        assertFalse(section.readBoolean());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testBoolean_true() {
        final FrameSection section = makeSection(0);
        section.writeBoolean(true);
        section.setFrame(frame);
        assertTrue(section.readBoolean());
    }

    @Test
    void testByte() {
        final byte value = -25;
        final FrameSection section = makeSection(10);
        section.writeByte(value);
        assertTrue(section.isAtEnd());

        section.setFrame(frame);
        assertEquals(value, section.readByte());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testUnsignedByte() {
        final byte value = -25;
        final FrameSection section = makeSection(5);
        section.writeByte(value);

        section.setFrame(frame);
        assertEquals(231, section.readUnsignedByte());
    }

    @Test
    void testShort() {
        final short value = -32767;
        final FrameSection section = makeSectionWithLength(2);
        section.writeShort(value);
        assertTrue(section.isAtEnd());

        section.setFrame(frame);
        assertEquals(value, section.readShort());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testUnsignedShort() {
        final short value = -32767;
        final FrameSection section = makeSectionWithLength(2);
        section.writeShort(value);

        section.setFrame(frame);
        assertEquals(32769, section.readUnsignedShort());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testInt() {
        final int value = -25;
        final FrameSection section = makeSectionWithLength(4);
        section.writeInt(value);
        assertTrue(section.isAtEnd());

        section.setFrame(frame);
        assertEquals(value, section.readInt());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testLong() {
        final long value = -25L;
        final FrameSection section = makeSectionWithLength(8);
        section.writeLong(value);
        assertTrue(section.isAtEnd());

        section.setFrame(frame);
        assertEquals(value, section.readLong());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testFloat() {
        final float value = -25f;
        final FrameSection section = makeSectionWithLength(4);
        section.writeFloat(value);
        assertTrue(section.isAtEnd());

        section.setFrame(frame);
        assertEquals(value, section.readFloat());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testDouble() {
        final double value = -25L;
        final FrameSection section = makeSectionWithLength(8);
        section.writeDouble(value);
        assertTrue(section.isAtEnd());

        section.setFrame(frame);
        assertEquals(value, section.readDouble());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testVector3f() {
        final Vector3f vector = new Vector3f(-1, -2, -3);
        final FrameSection section = makeSectionWithLength(12);
        section.writeVector3f(vector);
        assertTrue(section.isAtEnd());

        section.setFrame(frame);
        assertEquals(vector, section.readIntoVector3f(new Vector3f(3, 2, 1)));
        assertTrue(section.isAtEnd());
    }

    @Test
    void testSetFrame() {
        final FrameSection section = makeSection(1);
        section.readByte();
        assertTrue(section.isAtEnd());
        section.setFrame(new byte[1]);
        assertFalse(section.isAtEnd());
    }

    @Test
    void testIsAtEnd() {
        assertTrue(makeSectionWithLength(0).isAtEnd());
        assertFalse(makeSectionWithLength(1).isAtEnd());
    }
}
