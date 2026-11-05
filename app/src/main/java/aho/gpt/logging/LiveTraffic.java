package aho.gpt.logging;

import aho.gpt.custom.live.LiveStage;
import java.util.EnumMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Counts fixed event categories without retaining any event, identifier or content. */
public final class LiveTraffic {
    private enum Count {
        SENT_AUDIO, RECEIVED_AUDIO, INPUT_TRANSCRIPTS, VOICE_TRANSCRIPTS, DELEGATIONS,
        BACKEND_EVENTS, BACKEND_STARTED, BACKEND_COMPLETED, ITEMS_ADDED, ITEMS_DONE,
        TOOL_ITEMS_DONE, TOOL_RESULTS_SENT, CONTINUES_SENT, UNKNOWN_EVENTS
    }
    private static final Map<LiveStage, Count> STAGES = stages();
    private final Map<Count, Long> counts = new EnumMap<>(Count.class);
    private LiveStage last = LiveStage.OTHER;
    private long receivedAt;
    private int tools;
    private int history;
    private static Map<LiveStage, Count> stages() {
        Map<LiveStage, Count> stages = new EnumMap<>(LiveStage.class);
        stages.put(LiveStage.VOICE_AUDIO, Count.RECEIVED_AUDIO); stages.put(LiveStage.INPUT_TRANSCRIBED, Count.INPUT_TRANSCRIPTS);
        stages.put(LiveStage.VOICE_TRANSCRIPT, Count.VOICE_TRANSCRIPTS); stages.put(LiveStage.DELEGATED, Count.DELEGATIONS);
        stages.put(LiveStage.BACKEND_STARTED, Count.BACKEND_STARTED); stages.put(LiveStage.BACKEND_COMPLETED, Count.BACKEND_COMPLETED);
        stages.put(LiveStage.BACKEND_ITEM_ADDED, Count.ITEMS_ADDED); stages.put(LiveStage.BACKEND_ITEM_DONE, Count.ITEMS_DONE);
        stages.put(LiveStage.OTHER, Count.UNKNOWN_EVENTS); return stages;
    }
    public synchronized void sent(JSONObject event) {
        switch (LiveLogEvent.command(event)) {
            case "START": startup(event.optJSONObject("session")); break;
            case "INPUT_AUDIO": increment(Count.SENT_AUDIO); break;
            case "TOOL_RESULT": increment(Count.TOOL_RESULTS_SENT); break;
            case "CONTINUE": increment(Count.CONTINUES_SENT); break;
            default: break;
        }
    }
    private void startup(JSONObject session) {
        if (session == null) { return; }
        history = length(session.optJSONArray("input"));
        JSONObject delegation = session.optJSONObject("delegation");
        JSONObject responses = delegation == null ? null : delegation.optJSONObject("responses");
        tools = responses == null ? 0 : length(responses.optJSONArray("tools"));
    }
    public synchronized void received(JSONObject event, long now) {
        receivedAt = now; LiveStage stage = LiveStage.of(event);
        if (stage != LiveStage.OTHER) { last = stage; }
        Count count = STAGES.get(stage);
        if (count != null) { increment(count); }
        if ("response.event".equals(event.optString("type"))) {
            increment(Count.BACKEND_EVENTS); toolItem(event.optJSONObject("event"));
        }
    }
    private void toolItem(JSONObject event) {
        if (event == null || !"response.output_item.done".equals(event.optString("type"))) { return; }
        JSONObject item = event.optJSONObject("item");
        if (item != null && "function_call".equals(item.optString("type"))) { increment(Count.TOOL_ITEMS_DONE); }
    }
    private void increment(Count count) { counts.put(count, count(count) + 1); }
    private long count(Count count) { Long value = counts.get(count); return value == null ? 0 : value; }
    private int length(JSONArray values) { return values == null ? 0 : values.length(); }
    public synchronized String summary(long now) {
        StringBuilder result = new StringBuilder("lastStage=").append(last).append(" receiveIdleMs=")
                .append(receivedAt == 0 ? -1 : Math.max(0, now - receivedAt) / 1_000_000)
                .append(" advertisedTools=").append(tools).append(" historyItems=").append(history);
        for (Count count : Count.values()) { result.append(' ').append(count.name()).append('=').append(count(count)); }
        return result.toString();
    }
    public static String capture(byte[] pcm) {
        boolean signal = false;
        for (int index = 0; index + 1 < pcm.length; index += 2) {
            short sample = (short) ((pcm[index] & 255) | (pcm[index + 1] << 8));
            signal |= Math.abs((int) sample) > 180;
        }
        return "captureBytes=" + pcm.length + " captureDurationMs=" + pcm.length / 48 + " captureSignal=" + signal;
    }
}
