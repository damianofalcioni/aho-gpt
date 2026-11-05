package aho.gpt.android.net;

import aho.gpt.android.logging.AppLogger;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.live.LiveStage;
import aho.gpt.logging.LiveLogEvent;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;

/** Logs each fixed protocol stage once, never payloads, IDs, transcripts or audio. */
final class CustomLiveEvents implements RealtimeProtocol.Events {
    private static final String TAG = "Live";
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private final long connection = SEQUENCE.incrementAndGet();
    private final long started = System.nanoTime();
    private final RealtimeProtocol.Events events;
    private final Set<LiveStage> seen = EnumSet.noneOf(LiveStage.class);
    private boolean inputSent;
    CustomLiveEvents(RealtimeProtocol.Events events) { this.events = events; }
    @Override public void send(JSONObject event, RequestScope scope) throws Exception {
        try { events.send(event, scope); }
        catch (Exception failure) { AppLogger.w(TAG, prefix() + " transport=send_failed_or_cancelled", failure); throw failure; }
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
        catch (Exception failure) { AppLogger.w(TAG, prefix() + " transport=receive_failed_or_cancelled", failure); throw failure; }
        LiveStage stage = LiveStage.of(event);
        if (stage != LiveStage.OTHER && seen.add(stage)) { AppLogger.i(TAG, prefix() + " stage=" + stage.name()); }
        String failure = LiveLogEvent.failure(event);
        if (!failure.isEmpty()) { AppLogger.w(TAG, prefix() + " " + failure); }
        if (stage == LiveStage.CLOSED) { AppLogger.i(TAG, prefix() + " closeReason=" + LiveLogEvent.closeReason(event)); }
        return event;
    }
    private String prefix() { return "connection=" + connection + " elapsedMs=" + (System.nanoTime() - started) / 1_000_000; }
}
