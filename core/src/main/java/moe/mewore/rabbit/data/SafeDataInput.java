package moe.mewore.rabbit.data;

import java.io.DataInput;
import java.io.IOException;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A {@link DataInput} which does not throw any {@link IOException}s.
 */
public interface SafeDataInput extends DataInput {

    @Override
    void readFully(byte @NonNull [] b);

    @Override
    void readFully(byte @NonNull [] b, int off, int len);

    @Override
    int skipBytes(int n);

    @Override
    boolean readBoolean();

    @Override
    byte readByte();

    @Override
    int readUnsignedByte();

    @Override
    short readShort();

    @Override
    int readUnsignedShort();

    @Override
    char readChar();

    @Override
    int readInt();

    @Override
    long readLong();

    @Override
    float readFloat();

    @Override
    double readDouble();

    @Override
    String readLine();

    @Override
    String readUTF();
}
