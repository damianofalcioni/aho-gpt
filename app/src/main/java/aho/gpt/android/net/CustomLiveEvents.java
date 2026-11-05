package aho.gpt.android.net;

import aho.gpt.android.logging.AppLogger;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.RealtimeProtocol;
import aho.gpt.custom.live.LiveStage;
import java.util.EnumSet;
import java.util.Set;
import org.json.JSONObject;

/** Logs each fixed protocol stage once, never payloads, IDs, transcripts or audio. */
final class CustomLiveEvents implements RealtimeProtocol.Events {
    private final RealtimeProtocol.Events events;
    private final Set<LiveStage> seen = EnumSet.noneOf(LiveStage.class);
    CustomLiveEvents(RealtimeProtocol.Events events) { this.events = events; }
    @Override public void send(JSONObject event, RequestScope scope) throws Exception { events.send(event, scope); }
    @Override public JSONObject receive(RequestScope scope) throws Exception {
        JSONObject event = events.receive(scope); LiveStage stage = LiveStage.of(event);
        if (stage != LiveStage.OTHER && seen.add(stage)) { AppLogger.i("Live", "stage=" + stage.name()); }
        return event;
    }
}
