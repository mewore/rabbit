package moe.mewore.rabbit.backend.simulation.data;

import javax.vecmath.Vector3f;
import java.util.List;

import org.junit.jupiter.api.Test;

import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.BYTE;
import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.VECTOR3F;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameCompilerTest {

    @Test
    void testReserve() {
        final FrameCompiler frameCompiler = new FrameCompiler();
        final FrameSection section = frameCompiler.reserve(BYTE, VECTOR3F);
        assertEquals(13, frameCompiler.getSize());
        section.setFrame(new byte[frameCompiler.getSize()]);
        section.readByte();
        section.readIntoVector3f(new Vector3f());
        assertTrue(section.isAtEnd());

        frameCompiler.reserve(BYTE);
        assertEquals(14, frameCompiler.getSize());
    }

    @Test
    void testReserve_iterable() {
        final FrameCompiler frameCompiler = new FrameCompiler();
        final FrameSection section = frameCompiler.reserve(List.of(BYTE, VECTOR3F));
        assertEquals(13, frameCompiler.getSize());
        section.setFrame(new byte[frameCompiler.getSize()]);
        section.readByte();
        section.readIntoVector3f(new Vector3f());
        assertTrue(section.isAtEnd());
    }

    @Test
    void testReserveMultiple() {
        final FrameCompiler frameCompiler = new FrameCompiler();
        final FrameSection[] sections = frameCompiler.reserveMultiple(2, BYTE, VECTOR3F);
        assertEquals(2, sections.length);
        assertEquals(26, frameCompiler.getSize());
    }
}
