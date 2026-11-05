package aho.gpt.custom.live;

import aho.gpt.core.Conversation;
import aho.gpt.custom.SpeechApiConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static aho.gpt.custom.live.LiveFixture.*;
import static org.junit.Assert.*;

public class LiveHistoryTest {
    @Test public void followUpHistoryUsesRoleSpecificTextAndOmitsToolReceiptsAndAudio() throws Exception {
        Conversation history = new Conversation();
        history.begin("Earlier question", 1);
        history.append(new JSONArray().put(call())); history.toolResult("c1", "[]");
        history.append(new JSONArray().put(message())); history.answered(2);
        history.beginAudio(new JSONObject().put("role", "user").put("content",
                new JSONArray().put(new JSONObject().put(TYPE, "input_audio").put("data", "not retained"))), 3);
        JSONArray input = LiveSession.start(MODEL, SpeechApiConfig.DEFAULT, request(history))
                .getJSONObject("session").getJSONArray("input");
        assertEquals(2, input.length());
        assertEquals("user", input.getJSONObject(0).getString("role"));
        assertEquals("assistant", input.getJSONObject(1).getString("role"));
        assertPart(input.getJSONObject(0), "input_text", "Earlier question");
        assertPart(input.getJSONObject(1), "output_text", "Done");
        assertEquals(5, history.input().length());
    }
    private void assertPart(JSONObject message, String type, String text) throws Exception {
        JSONArray content = message.getJSONArray("content"); assertEquals(1, content.length());
        assertEquals(type, content.getJSONObject(0).getString(TYPE));
        assertEquals(text, content.getJSONObject(0).getString("text"));
    }
}
