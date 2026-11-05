package aho.gpt.logging;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class LiveTrafficTest {
    private static final String TYPE = "type";
    private static final String PRIVATE = "Bearer secret\nprivate request, answer and screen text https://private.example";
    @Test public void distinguishesToolRequestsFromReceiptsAndContinuationWithoutCopyingPayloads() throws Exception {
        LiveTraffic traffic = new LiveTraffic();
        JSONObject session = new JSONObject().put("input", new JSONArray().put(PRIVATE)).put("delegation",
                new JSONObject().put("responses", new JSONObject().put("tools", new JSONArray().put(PRIVATE).put(PRIVATE))));
        traffic.sent(event("session.start").put("session", session));
        traffic.received(event("session.delegation.created").put("delegation_id", PRIVATE), 1_000_000);
        traffic.received(nested(event("response.created").put("response", new JSONObject().put("id", PRIVATE))), 2_000_000);
        JSONObject item = new JSONObject().put(TYPE, "function_call").put("name", PRIVATE).put("arguments", PRIVATE);
        traffic.received(nested(event("response.output_item.added").put("item", item)), 3_000_000);
        traffic.received(nested(event("response.output_item.done").put("item", item)), 4_000_000);
        String pending = traffic.summary(5_000_000);
        assertTrue(pending.contains("advertisedTools=2 historyItems=1"));
        assertTrue(pending.contains("BACKEND_EVENTS=3")); assertTrue(pending.contains("ITEMS_DONE=1 TOOL_ITEMS_DONE=1"));
        assertTrue(pending.contains("TOOL_RESULTS_SENT=0 CONTINUES_SENT=0"));
        assertFalse(pending.contains("secret")); assertFalse(pending.contains("private")); assertFalse(pending.contains("\n"));
        traffic.sent(event("response.item.create").put("item", new JSONObject().put("output", PRIVATE)));
        traffic.sent(event("response.create"));
        assertTrue(traffic.summary(5_000_000).contains("TOOL_RESULTS_SENT=1 CONTINUES_SENT=1"));
    }
    @Test public void silenceAndMissingTranscriptionRemainVisibleInAggregateCounts() throws Exception {
        LiveTraffic traffic = new LiveTraffic();
        JSONObject input = event("session.input_audio.append").put("audio", PRIVATE);
        for (int index = 0; index < 601; index++) { traffic.sent(input); }
        traffic.received(event("session.output_audio.delta").put("delta", PRIVATE), 1_000_000);
        traffic.received(event("session.output_audio.delta").put("delta", PRIVATE), 2_000_000);
        String summary = traffic.summary(60_002_000_000L);
        assertTrue(summary.contains("lastStage=VOICE_AUDIO receiveIdleMs=60000"));
        assertTrue(summary.contains("SENT_AUDIO=601 RECEIVED_AUDIO=2 INPUT_TRANSCRIPTS=0 VOICE_TRANSCRIPTS=0 DELEGATIONS=0 BACKEND_EVENTS=0"));
        assertFalse(summary.contains(PRIVATE));
    }
    @Test public void unknownAndMalformedEventsCannotExposeProviderNamesOrContent() throws Exception {
        LiveTraffic traffic = new LiveTraffic();
        traffic.received(event(PRIVATE), 1_000_000);
        traffic.received(nested(event(PRIVATE)), 2_000_000);
        traffic.received(event("response.event").put("event", PRIVATE), 3_000_000);
        traffic.sent(event("session.start").put("session", PRIVATE));
        String summary = traffic.summary(4_000_000);
        assertTrue(summary.contains("UNKNOWN_EVENTS=3")); assertTrue(summary.contains("BACKEND_EVENTS=2"));
        assertFalse(summary.contains("private")); assertFalse(summary.contains("Bearer"));
        assertFalse(summary.contains("http")); assertFalse(summary.contains("\n"));
    }
    @Test public void captureSummaryDistinguishesSignalFromSilenceWithoutRetainingAudio() {
        byte[] pcm = new byte[48_000];
        assertEquals("captureBytes=48000 captureDurationMs=1000 captureSignal=false", LiveTraffic.capture(pcm));
        pcm[1] = (byte) 128;
        assertEquals("captureBytes=48000 captureDurationMs=1000 captureSignal=true", LiveTraffic.capture(pcm));
        pcm[1] = 0;
        assertEquals("captureBytes=48000 captureDurationMs=1000 captureSignal=false", LiveTraffic.capture(pcm));
    }
    private JSONObject event(String type) throws Exception { return new JSONObject().put(TYPE, type); }
    private JSONObject nested(JSONObject event) throws Exception {
        return event("response.event").put("event", event).put("delegation_id", PRIVATE);
    }
}
