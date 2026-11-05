package aho.gpt.custom.live;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.realtime.WireBase64;
import org.json.JSONObject;

/** Silence advances the Live timeline after foreground capture has stopped. */
public final class LiveInputPump implements AutoCloseable {
    private Thread worker;
    public void start(RealtimeProtocol.Events events, byte[] pcm, RequestScope scope, Runnable failed) throws Exception {
        scope.check(); events.send(audio(pcm), scope);
        worker = new Thread(() -> {
            try {
                JSONObject silence = audio(new byte[4800]);
                while (true) { Thread.sleep(100); scope.check(); events.send(silence, scope); }
            } catch (InterruptedException ignored) { /* Closing never sends another captured turn. */ }
            catch (Exception exception) { failed.run(); }
        }, "Live silence");
        worker.setDaemon(true); worker.start();
    }
    private JSONObject audio(byte[] pcm) throws Exception {
        return new JSONObject().put("type", "session.input_audio.append").put("audio", WireBase64.encode(pcm));
    }
    @Override public void close() { if (worker != null) { worker.interrupt(); worker = null; } }
}
