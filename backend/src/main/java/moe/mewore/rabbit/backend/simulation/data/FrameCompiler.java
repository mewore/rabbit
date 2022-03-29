package moe.mewore.rabbit.backend.simulation.data;

import java.util.Arrays;
import java.util.stream.StreamSupport;

import lombok.Getter;

public class FrameCompiler {

    @Getter
    private int size = 0;

    public FrameSection reserve(final FrameDataType... elementTypes) {
        return reserve(Arrays.stream(elementTypes).mapToInt(FrameDataType::getByteSize).sum());
    }

    public FrameSection reserve(final Iterable<FrameDataType> elementTypes) {
        return reserve(
            StreamSupport.stream(elementTypes.spliterator(), true).mapToInt(FrameDataType::getByteSize).sum());
    }

    private FrameSection reserve(final int reservationSize) {
        final FrameSection result = new FrameSection(size, reservationSize);
        size += reservationSize;
        return result;
    }

    public FrameSection[] reserveMultiple(final int count, final FrameDataType... elementTypes) {
        final FrameSection[] result = new FrameSection[count];
        for (int i = 0; i < count; i++) {
            result[i] = reserve(elementTypes);
        }
        return result;
    }
}
