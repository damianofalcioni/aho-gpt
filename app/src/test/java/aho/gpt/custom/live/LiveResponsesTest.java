package aho.gpt.custom.live;

import java.io.IOException;
import org.json.JSONObject;
import org.junit.Test;
import static aho.gpt.custom.live.LiveFixture.*;
import static org.junit.Assert.*;

public class LiveResponsesTest {
    private static final String DONE = "response.output_item.done";
    private static final String ADDED = "response.output_item.added";
    private static final String RESPONSE_ID = "response_id";
    private static final String COMPLETED = "completed";
    @Test public void partialArgumentsAreIgnoredUntilCompletedBackendResponse() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.response(COMPLETED, call()); LiveResponses responses = new LiveResponses();
        assertNull(responses.consume(fixture.queue.remove()));
        assertNull(responses.consume(nested(event("response.function_call_arguments.delta").put("delta", "{"))));
        assertNull(responses.consume(fixture.queue.remove())); assertNull(responses.consume(fixture.queue.remove()));
        assertEquals(1, responses.consume(fixture.queue.remove()).getJSONArray("output").length());
    }
    @Test public void foreignResponseAndDelegationIdsAreRejected() throws Exception {
        LiveResponses responses = started(); JSONObject item = item(ADDED, 0, call());
        responses.consume(nested(item));
        assertThrows(IOException.class, () -> responses.consume(nested(item(DONE, 0, call()).put(RESPONSE_ID, "foreign"))));
        assertThrows(IOException.class, () -> responses.consume(nested(item(DONE, 0, call())).put("delegation_id", "foreign")));
    }
    @Test public void optionalResponseIdsMustStillMatchWhenProvided() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.response(COMPLETED, call()); LiveResponses responses = new LiveResponses();
        responses.consume(fixture.queue.remove());
        responses.consume(fixture.queue.remove().put("event", item(ADDED, 0, call()).put(RESPONSE_ID, "r1")));
        responses.consume(fixture.queue.remove().put("event", item(DONE, 0, call()).put(RESPONSE_ID, "r1")));
        assertEquals(1, responses.consume(fixture.queue.remove()).getJSONArray("output").length());
    }
    @Test public void unownedItemsAndMismatchedTerminalResponsesAreRejected() throws Exception {
        assertThrows(IOException.class, () -> new LiveResponses().consume(nested(item(ADDED, 0, call()))));
        LiveResponses responses = started();
        responses.consume(nested(item(ADDED, 0, call()))); responses.consume(nested(item(DONE, 0, call())));
        JSONObject completed = event("response.completed").put("response", new JSONObject().put("id", "foreign").put("status", COMPLETED));
        assertThrows(IOException.class, () -> responses.consume(nested(completed)));
    }
    @Test public void missingOrDuplicateItemsCannotLookComplete() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.response(COMPLETED, call()); LiveResponses responses = new LiveResponses();
        responses.consume(fixture.queue.remove()); responses.consume(fixture.queue.remove());
        fixture.queue.remove();
        assertThrows(IOException.class, () -> responses.consume(fixture.queue.remove()));
        LiveResponses duplicate = started(); duplicate.consume(nested(item(ADDED, 0, call()))); duplicate.consume(nested(item(DONE, 0, call())));
        assertThrows(IOException.class, () -> duplicate.consume(nested(item(DONE, 0, call()))));
    }
    @Test public void failedResponsesAndUnexpectedDisconnectsAreNotAnswers() throws Exception {
        LiveResponses responses = started();
        assertThrows(IOException.class, () -> responses.consume(nested(event("response.failed"))));
        LiveFixture fixture = new LiveFixture(); fixture.queue.add(event("session.closed"));
        assertThrows(IOException.class, () -> new LiveProtocol(fixture).respond(request(new aho.gpt.core.Conversation()), new aho.gpt.core.RequestScope()));
    }
    private LiveResponses started() throws Exception {
        LiveResponses responses = new LiveResponses();
        responses.consume(nested(event("response.created").put("response", new JSONObject().put("id", "r1")))); return responses;
    }
}
