package aho.gpt.logging;

import org.json.JSONArray;
import org.json.JSONObject;
import aho.gpt.custom.live.LiveStage;
import org.junit.Test;
import static org.junit.Assert.*;

public class LiveLogEventTest {
    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String RESPONSE = "response";
    private static final String STATUS = "status";
    private static final String CODE = "code";
    private static final String PARAM = "param";
    private static final String REASON = "reason";
    private static final String PRIVATE = "Bearer private-token\nprivate screen text https://private.example";
    private static final String NO_ERROR = " errorType=none errorCode=none errorParam=none";

    @Test public void recordsTheRejectedFieldWithoutProviderTextOrIdentity() throws Exception {
        JSONObject error = new JSONObject().put(TYPE, "invalid_request_error").put(CODE, "unknown_parameter")
                .put(PARAM, "session.voice").put("message", PRIVATE).put("client_event_id", PRIVATE);
        JSONObject event = event(ERROR).put(ERROR, error).put("event_id", PRIVATE);
        String original = event.toString();
        assertEquals("source=session errorType=invalid_request_error errorCode=unknown_parameter errorParam=session.voice",
                LiveLogEvent.failure(event));
        assertEquals(original, event.toString());
    }
    @Test public void untrustedMetadataCannotBecomeLogText() throws Exception {
        JSONObject error = new JSONObject().put(TYPE, PRIVATE).put(CODE, PRIVATE).put(PARAM, PRIVATE);
        assertEquals("source=session errorType=other errorCode=other errorParam=other", LiveLogEvent.failure(event(ERROR).put(ERROR, error)));
        error.put(TYPE, new JSONObject().put("text", PRIVATE)).put(CODE, new JSONArray().put(PRIVATE)).put(PARAM, 12);
        assertEquals("source=session errorType=other errorCode=other errorParam=other", LiveLogEvent.failure(event(ERROR).put(ERROR, error)));
    }
    @Test public void missingNullAndMalformedErrorsHaveExplicitSafeDefaults() throws Exception {
        assertEquals("source=session" + NO_ERROR, LiveLogEvent.failure(event(ERROR)));
        assertEquals("source=session" + NO_ERROR, LiveLogEvent.failure(event(ERROR).put(ERROR, PRIVATE)));
        JSONObject error = new JSONObject().put(TYPE, JSONObject.NULL).put(CODE, JSONObject.NULL).put(PARAM, JSONObject.NULL);
        assertEquals("source=session" + NO_ERROR, LiveLogEvent.failure(event(ERROR).put(ERROR, error)));
        error.put(PARAM, "");
        assertEquals("source=session errorType=none errorCode=none errorParam=other", LiveLogEvent.failure(event(ERROR).put(ERROR, error)));
    }
    @Test public void recordsNestedBackendFailureAndIncompleteReason() throws Exception {
        JSONObject response = new JSONObject().put(STATUS, "failed").put(ERROR,
                new JSONObject().put(CODE, "server_error").put("message", PRIVATE));
        assertEquals("source=backend status=failed errorType=none errorCode=server_error errorParam=none incompleteReason=none",
                LiveLogEvent.failure(nested(event("response.failed").put(RESPONSE, response))));
        response.remove(ERROR);
        response.put(STATUS, "incomplete").put("incomplete_details", new JSONObject().put(REASON, "max_output_tokens"));
        assertEquals("source=backend status=incomplete" + NO_ERROR + " incompleteReason=max_output_tokens",
                LiveLogEvent.failure(nested(event("response.incomplete").put(RESPONSE, response))));
        assertEquals("source=backend" + NO_ERROR, LiveLogEvent.failure(nested(event(ERROR))));
    }
    @Test public void malformedBackendFailuresCannotLeakOrPretendToSucceed() throws Exception {
        JSONObject response = new JSONObject().put(STATUS, PRIVATE).put("incomplete_details", new JSONObject().put(REASON, PRIVATE));
        assertEquals("source=backend status=other" + NO_ERROR + " incompleteReason=other",
                LiveLogEvent.failure(nested(event("response.completed").put(RESPONSE, response))));
        assertEquals("source=backend status=none" + NO_ERROR + " incompleteReason=none",
                LiveLogEvent.failure(nested(event("response.cancelled"))));
    }
    @Test public void normalEventsAndUnknownEventNamesDoNotProduceErrors() throws Exception {
        assertEquals("", LiveLogEvent.failure(nested(event("response.completed").put(RESPONSE, new JSONObject().put(STATUS, "completed")))));
        assertEquals("", LiveLogEvent.failure(event("session.input_transcript.delta").put("delta", PRIVATE)));
        assertEquals("", LiveLogEvent.failure(event("response.event").put("event", PRIVATE)));
        assertEquals("", LiveLogEvent.failure(nested(event(PRIVATE))));
    }
    @Test public void commandAndCloseMetadataUseFixedVocabularyOnly() throws Exception {
        String[] types = {"session.start", "session.input_audio.append", "response.item.create", "response.create", "session.close", PRIVATE};
        String[] commands = {"START", "INPUT_AUDIO", "TOOL_RESULT", "CONTINUE", "CLOSE", "OTHER"};
        for (int index = 0; index < types.length; index++) { assertEquals(commands[index], LiveLogEvent.command(event(types[index]).put("audio", PRIVATE))); }
        assertEquals("expired", LiveLogEvent.closeReason(event("session.closed").put(REASON, "expired")));
        assertEquals("other", LiveLogEvent.closeReason(event("session.closed").put(REASON, PRIVATE)));
        assertEquals("none", LiveLogEvent.closeReason(new JSONObject()));
    }
    @Test public void nestedFailuresUseTheFailedStageAndUnknownStagesStayPrivate() throws Exception {
        for (String type : new String[]{"response.failed", "response.incomplete", "response.cancelled", ERROR}) {
            assertEquals(LiveStage.FAILED, LiveStage.of(nested(event(type))));
        }
        assertEquals(LiveStage.OTHER, LiveStage.of(nested(event(PRIVATE))));
        assertEquals(LiveStage.OTHER, LiveStage.of(event("response.event")));
    }
    private JSONObject event(String type) throws Exception { return new JSONObject().put(TYPE, type); }
    private JSONObject nested(JSONObject event) throws Exception {
        return event("response.event").put("event", event).put("delegation_id", PRIVATE);
    }
}
