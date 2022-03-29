package moe.mewore.rabbit.backend.simulation.data;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FrameSerializationTestUtil {

    public static void testSerialization(final FrameCompiler frameCompiler, final FrameSection frameSection,
        final FrameSerializableEntity entity, final FrameSerializableEntity secondEntity) {

        testSerialization(() -> new byte[frameCompiler.getSize()], frameSection, frameSection, entity, secondEntity);
    }

    public static void testSerialization(final Supplier<byte[]> frameSupplier, final FrameSection firstFrameSection,
        final FrameSection secondFrameSection, final FrameSerializableEntity entity,
        final FrameSerializableEntity secondEntity) {

        final byte[][] frames = new byte[][]{frameSupplier.get(), frameSupplier.get()};
        Arrays.fill(frames[0], (byte) 1);
        Arrays.fill(frames[1], (byte) 2);

        assertFalse(firstFrameSection.isAtEnd());
        entity.store(frames[0]);
        assertTrue(firstFrameSection.isAtEnd());

        secondEntity.store(frames[1]);
        // Just to make sure that the two entities are, in fact, with a different initial serialization
        assertNotEquals(List.of(frames[0]), List.of(frames[1]));

        secondEntity.load(frames[0]);
        assertTrue(secondFrameSection.isAtEnd());

        secondEntity.store(frames[1]);

        assertArrayEquals(frames[0], frames[1]);
    }
}
