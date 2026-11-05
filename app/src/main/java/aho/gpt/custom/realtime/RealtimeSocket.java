package aho.gpt.custom.realtime;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.ServerEndpoint;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONObject;

/** One configured host; TLS verifies its hostname. Closing interrupts pending reads. */
public final class RealtimeSocket implements RealtimeProtocol.Events, AutoCloseable {
    private static final Executor CLOSER = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "Voice socket close"); thread.setDaemon(true); return thread;
    });
    private Socket active;
    private WebSocketFrames frames;
    public void connect(String base, String model, String token, RequestScope scope) throws Exception {
        connect(base, model, token, false, scope);
    }
    public void connectLive(String base, String token, RequestScope scope) throws Exception {
        connect(base, "", token, true, scope);
    }
    private void connect(String base, String model, String token, boolean live, RequestScope scope) throws Exception {
        URI uri = URI.create(ServerEndpoint.normalize(base));
        boolean tls = "https".equals(uri.getScheme());
        int port = uri.getPort() < 0 ? (tls ? 443 : 80) : uri.getPort();
        Socket socket = new Socket(); activate(socket, scope);
        socket.connect(new InetSocketAddress(uri.getHost(), port), 15_000); socket.setSoTimeout(60_000);
        Socket connected = tls ? secure(socket, uri.getHost(), port, scope) : socket;
        if (live) { WebSocketHandshake.upgradePath(uri, uri.getRawPath() + "/live/sessions", token, connected.getInputStream(), connected.getOutputStream()); }
        else { WebSocketHandshake.upgrade(uri, model, token, connected.getInputStream(), connected.getOutputStream()); }
        scope.check(); frames = new WebSocketFrames(connected.getInputStream(), connected.getOutputStream());
    }
    private synchronized void activate(Socket socket, RequestScope scope) throws Exception {
        try { scope.check(); active = socket; }
        catch (InterruptedException exception) { socket.close(); throw exception; }
    }
    private Socket secure(Socket socket, String host, int port, RequestScope scope) throws Exception {
        SSLSocket secure = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(socket, host, port, true);
        activate(secure, scope);
        secure.setSoTimeout(60_000); secure.startHandshake();
        if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, secure.getSession())) {
            throw new IOException("Realtime TLS hostname mismatch");
        }
        return secure;
    }
    @Override public void send(JSONObject event, RequestScope scope) throws Exception { scope.check(); frames.text(event.toString()); }
    @Override public JSONObject receive(RequestScope scope) throws Exception { return new JSONObject(frames.text(scope)); }
    public synchronized void readTimeout(int milliseconds) throws IOException { if (active != null) { active.setSoTimeout(milliseconds); } }
    @Override public synchronized void close() {
        Socket socket = active;
        active = null;
        if (socket != null) {
            // TLS close can write to the network. Cancellation may originate on Android's UI thread.
            CLOSER.execute(() -> close(socket));
        }
    }
    private static void close(Socket socket) {
        try { socket.close(); }
        catch (IOException ignored) { /* Cancellation is best effort; the scope remains revoked. */ }
    }
}
