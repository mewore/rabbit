package moe.mewore.rabbit.backend.mock.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.WriteCallback;

import moe.mewore.rabbit.backend.mock.FakeEmptyFuture;

public class FakeRemoteEndpoint implements RemoteEndpoint {

    private final List<byte[]> sentData = new ArrayList<>();

    private ByteArrayOutputStream currentFragment = new ByteArrayOutputStream();

    public List<byte[]> getSentData() {
        return Collections.unmodifiableList(sentData);
    }

    @Override
    public void sendBytes(final ByteBuffer data) {
        sentData.add(data.array());
    }

    @Override
    public Future<Void> sendBytesByFuture(final ByteBuffer data) {
        sentData.add(data.array());
        return new FakeEmptyFuture();
    }

    @Override
    public void sendBytes(final ByteBuffer data, final WriteCallback callback) {
        sentData.add(data.array());
        callback.writeSuccess();
    }

    @Override
    public void sendPartialBytes(final ByteBuffer fragment, final boolean isLast) throws IOException {
        currentFragment.write(fragment.array());
        if (isLast) {
            sentData.add(currentFragment.toByteArray());
            currentFragment = new ByteArrayOutputStream();
        }
    }

    @Override
    public void sendPartialString(final String fragment, final boolean isLast) {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public void sendPing(final ByteBuffer applicationData) {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public void sendPong(final ByteBuffer applicationData) {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public void sendString(final String text) {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public Future<Void> sendStringByFuture(final String text) {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public void sendString(final String text, final WriteCallback callback) {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public BatchMode getBatchMode() {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public void setBatchMode(final BatchMode mode) {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public int getMaxOutgoingFrames() {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public void setMaxOutgoingFrames(final int maxOutgoingFrames) {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        throw new UnsupportedOperationException("Fake method not implemented");
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("Fake method not implemented");
    }
}
