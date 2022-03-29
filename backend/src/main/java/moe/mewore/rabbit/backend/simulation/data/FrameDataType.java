package moe.mewore.rabbit.backend.simulation.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FrameDataType {

    BYTE(Byte.BYTES),
    SHORT(Short.BYTES),
    INTEGER(Integer.BYTES),
    LONG(Long.BYTES),
    FLOAT(Float.BYTES),
    DOUBLE(Double.BYTES),
    VECTOR3F(3 * Float.BYTES);

    @Getter(AccessLevel.PACKAGE)
    private final int byteSize;
}
