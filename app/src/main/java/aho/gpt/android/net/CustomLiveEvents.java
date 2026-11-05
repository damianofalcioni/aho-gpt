package aho.gpt.android.net;

import aho.gpt.android.logging.AppLogger;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.live.LiveStage;
import aho.gpt.custom.live.LiveProtocol;
import aho.gpt.custom.live.LiveInactivityException;
import aho.gpt.logging.LiveLogEvent;
import aho.gpt.logging.LiveTraffic;
import java.net.SocketTimeoutException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Logs each fixed protocol stage once, never payloads, IDs, transcripts or audio. */
final class CustomLiveEvents implements RealtimeProtocol.Events {
    private enum Outcome { COMPLETED, CANCELLED, INACTIVITY_TIMEOUT, TRANSPORT_TIMEOUT, FAILED }
    private static final String TAG = "Live";
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private final long connection = SEQUENCE.incrementAndGet();
    private final long started = System.nanoTime();
    private final RealtimeProtocol.Events events;
    private final Set<LiveStage> seen = EnumSet.noneOf(LiveStage.class);
    private final LiveTraffic traffic = new LiveTraffic();
    private final AtomicBoolean ended = new AtomicBoolean();
    private LiveProtocol protocol;
    private long heartbeat = started;
    private boolean inputSent;
    CustomLiveEvents(RealtimeProtocol.Events events) { this.events = events; }
    void attach(LiveProtocol value) { protocol = value; AppLogger.i(TAG, prefix() + " transport=connecting"); }
    void capture(byte[] pcm) {
        if (AppLogger.isLoggingEnabled()) { AppLogger.i(TAG, prefix() + " " + LiveTraffic.capture(pcm)); }
    }
    void checkpoint() {
        if (AppLogger.isLoggingEnabled()) { AppLogger.i(TAG, prefix() + " checkpoint " + snapshot()); }
    }
    void completed() { end(Outcome.COMPLETED); }
    void cancelled() { end(Outcome.CANCELLED); }
    void failed(Exception failure, RequestScope scope) {
        if (scope.isCancelled()) { cancelled(); return; }
        Outcome outcome = failure instanceof SocketTimeoutException ? Outcome.TRANSPORT_TIMEOUT : Outcome.FAILED;
        end(failure instanceof LiveInactivityException ? Outcome.INACTIVITY_TIMEOUT : outcome);
    }
    private void end(Outcome outcome) {
        if (!ended.compareAndSet(false, true)) { return; }
        String state = outcome == Outcome.CANCELLED ? "phase=CANCELLED " + traffic.summary(System.nanoTime()) : snapshot();
        String message = prefix() + " outcome=" + outcome + " " + state;
        if (outcome == Outcome.COMPLETED || outcome == Outcome.CANCELLED) { AppLogger.i(TAG, message); }
        else { AppLogger.w(TAG, message); }
    }
    @Override public void send(JSONObject event, RequestScope scope) throws Exception {
        try { events.send(event, scope); }
        catch (Exception failure) { transportFailure("send", failure, scope); throw failure; }
        traffic.sent(event);
        String command = LiveLogEvent.command(event);
        if ("INPUT_AUDIO".equals(command)) {
            if (inputSent) { return; }
            inputSent = true;
        }
        if (!"OTHER".equals(command)) { AppLogger.i(TAG, prefix() + " command=" + command); }
    }
    @Override public JSONObject receive(RequestScope scope) throws Exception {
        JSONObject event;
        try { event = events.receive(scope); }
        catch (Exception failure) { transportFailure("receive", failure, scope); throw failure; }
        traffic.received(event, System.nanoTime());
        LiveStage stage = LiveStage.of(event);
        if (stage != LiveStage.OTHER && seen.add(stage)) { AppLogger.i(TAG, prefix() + " stage=" + stage.name()); }
        String failure = LiveLogEvent.failure(event);
        if (!failure.isEmpty()) { AppLogger.w(TAG, prefix() + " " + failure); }
        if (stage == LiveStage.CLOSED) { AppLogger.i(TAG, prefix() + " closeReason=" + LiveLogEvent.closeReason(event)); }
        heartbeat();
        return event;
    }
    private void transportFailure(String operation, Exception failure, RequestScope scope) {
        if (scope.isCancelled()) { AppLogger.d(TAG, prefix() + " transport=" + operation + "_cancelled"); }
        else { AppLogger.w(TAG, prefix() + " transport=" + operation + "_failed", failure); }
    }
    private void heartbeat() {
        long now = System.nanoTime();
        if (!ended.get() && now - heartbeat >= 10_000_000_000L) {
            heartbeat = now; AppLogger.d(TAG, prefix() + " heartbeat " + snapshot());
        }
    }
    private String snapshot() {
        String state = protocol == null ? "phase=CONNECTING" : protocol.diagnostics();
        return state + " " + traffic.summary(System.nanoTime());
    }
    private String prefix() { return "connection=" + connection + " elapsedMs=" + (System.nanoTime() - started) / 1_000_000; }
}
