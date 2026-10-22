package dev.lightassistant;

import dev.lightassistant.core.Conversation;
import dev.lightassistant.core.FollowUpWindow;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversationTest {
    @Test public void silenceExpiresExactlyAtDeadline() {
        FollowUpWindow window = new FollowUpWindow();
        assertFalse(window.isOpen(1)); window.open(1000);
        assertTrue(window.isOpen(8999)); assertFalse(window.isOpen(9000));
        assertEquals(0, window.remaining(9100));
    }
    @Test public void followUpKeepsHistoryAndNewSessionDropsIt() throws Exception {
        Conversation conversation = new Conversation();
        conversation.begin("Open music", 100);
        conversation.append(new JSONArray().put(new JSONObject().put("role", "assistant").put("content", "Which app?")));
        conversation.answered(1000); conversation.begin("The radio app", 8999);
        assertEquals(3, conversation.input().length());
        conversation.answered(9000); conversation.begin("Set a timer", 17000);
        assertEquals(1, conversation.input().length());
    }
    @Test public void explicitCloseDropsPendingToolHistory() throws Exception {
        Conversation conversation = new Conversation();
        conversation.begin("Read", 1); conversation.toolResult("call", "screen text");
        conversation.clear(); assertEquals(0, conversation.input().length());
    }
}
