package moe.mewore.rabbit.entities;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

import moe.mewore.rabbit.data.ByteArrayDataOutput;
import moe.mewore.rabbit.data.SafeDataOutput;

public abstract class BinaryEntity {

    public byte[] encodeToBinary() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final SafeDataOutput output = new ByteArrayDataOutput(outputStream);
        appendToBinaryOutput(output);
        return outputStream.toByteArray();
    }

    protected void appendCollectionToBinaryOutput(final Collection<? extends BinaryEntity> collection,
        final SafeDataOutput output) {
        output.writeInt(collection.size());
        for (final BinaryEntity binaryEntity : collection) {
            binaryEntity.appendToBinaryOutput(output);
        }
    }

    public abstract void appendToBinaryOutput(final SafeDataOutput output);
}
