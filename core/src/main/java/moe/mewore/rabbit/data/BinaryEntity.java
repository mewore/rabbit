package moe.mewore.rabbit.data;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

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
