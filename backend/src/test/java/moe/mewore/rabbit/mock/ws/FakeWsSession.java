package moe.mewore.rabbit.mock.ws;

import java.net.InetSocketAddress;
import java.util.List;

import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.SuspendToken;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FakeWsSession implements Session {

    private final String id;

    private final FakeRemoteEndpoint remote = new FakeRemoteEndpoint();

    private boolean open = true;

    public List<byte[]> getSentData() {
        return remote.getSentData();
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public void close(final CloseStatus closeStatus) {
        open = false;
    }

    @Override
    public void close(final int statusCode, final String reason) {
        open = false;
    }

    @Override
    public void disconnect() {
        open = false;
    }

    @Override
    public long getIdleTimeout() {
        return 0;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public WebSocketPolicy getPolicy() {
        return null;
    }

    @Override
    public String getProtocolVersion() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public UpgradeRequest getUpgradeRequest() {
        return null;
    }

    @Override
    public UpgradeResponse getUpgradeResponse() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public void setIdleTimeout(final long ms) {

    }

    @Override
    public SuspendToken suspend() {
        return null;
    }
}
