package aho.gpt.custom.live;

import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.WireBase64;
import aho.gpt.openai.ResponseText;
import java.io.IOException;
import org.json.JSONObject;
import org.junit.Test;
import static aho.gpt.custom.live.LiveFixture.*;
import static org.junit.Assert.*;

public class LiveDirectAnswerTest {
    private static final String DELTA = "delta";
    private static final String ANSWER = "Four.";
    @Test public void directSpokenAnswerFinishesWithoutADelegatedResponse() throws Exception {
        LiveFixture fixture = new LiveFixture(); spoken(fixture, ANSWER);
        LiveProtocol protocol = new LiveProtocol(fixture); RequestScope scope = new RequestScope();
        JSONObject answer = protocol.respond(request(new Conversation()), scope);
        assertEquals(ANSWER, ResponseText.extract(answer.getJSONArray("output")));
        assertTrue(protocol.diagnostics().contains("phase=DIRECT_ANSWER_READY"));
        assertTrue(protocol.diagnostics().contains("outputSpeech=true outputSettled=true"));
        assertTrue(protocol.diagnostics().contains("delegated=false voiceText=true"));
        assertFalse(protocol.diagnostics().contains(ANSWER));
        assertEquals(96_002, protocol.finish(true, scope).length);
        assertTrue(fixture.sent.isEmpty());
    }
    @Test public void spokenAcknowledgementCannotCompleteDelegatedWork() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.queue.add(event("session.delegation.created"));
        spoken(fixture, "Checking."); fixture.response("completed", call());
        LiveProtocol protocol = new LiveProtocol(fixture);
        JSONObject response = protocol.respond(request(new Conversation()), new RequestScope());
        assertEquals("function_call", response.getJSONArray("output").getJSONObject(0).getString(TYPE));
        assertTrue(protocol.diagnostics().contains("phase=WAITING_TOOL_RESULTS"));
        assertTrue(protocol.diagnostics().contains("delegated=true voiceText=true"));
        assertFalse(protocol.diagnostics().contains("Checking."));
    }
    @Test public void failedBackendCannotBeReplacedWithEarlierSpeech() throws Exception {
        LiveFixture fixture = new LiveFixture(); fixture.queue.add(event("session.delegation.created"));
        spoken(fixture, "Checking."); fixture.response("incomplete", call());
        assertThrows(IOException.class, () -> new LiveProtocol(fixture).respond(request(new Conversation()), new RequestScope()));
    }
    @Test public void transcriptWithoutCompletedSpeechIsNotAnAnswer() throws Exception {
        LiveFixture fixture = new LiveFixture();
        fixture.queue.add(event("session.output_transcript.delta").put(DELTA, ANSWER)); fixture.queue.add(event("session.closed"));
        assertThrows(IOException.class, () -> new LiveProtocol(fixture).respond(request(new Conversation()), new RequestScope()));
    }
    @Test public void directTranscriptsStayBoundedAndCannotContainTools() throws Exception {
        LiveDirectAnswer answer = new LiveDirectAnswer();
        assertThrows(IOException.class, () -> answer.consume(event("session.output_transcript.delta").put(DELTA, "x".repeat(80_001))));
        assertEquals(LiveStage.OTHER, LiveStage.of(event("private-provider-data")));
    }
    private void spoken(LiveFixture fixture, String text) throws Exception {
        fixture.queue.add(event("session.output_transcript.delta").put(DELTA, text));
        fixture.queue.add(event("session.output_audio.delta").put(DELTA, WireBase64.encode(new byte[]{0, 20})));
        fixture.queue.add(event("session.output_audio.delta").put(DELTA, WireBase64.encode(new byte[96_000])));
    }
}
