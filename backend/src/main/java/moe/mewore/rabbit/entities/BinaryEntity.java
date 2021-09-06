package moe.mewore.rabbit.entities;

import java.io.ByteArrayOutputStream;

import moe.mewore.rabbit.data.ByteArrayDataOutput;
import moe.mewore.rabbit.data.SafeDataOutput;

public abstract class BinaryEntity {

    public byte[] encodeToBinary() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final SafeDataOutput output = new ByteArrayDataOutput(outputStream);
        appendToBinaryOutput(output);
        return outputStream.toByteArray();
    }

    public abstract void appendToBinaryOutput(final SafeDataOutput output);
}
