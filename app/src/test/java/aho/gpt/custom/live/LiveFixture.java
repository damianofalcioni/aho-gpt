package aho.gpt.custom.live;

import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.SpeechApiConfig;
import aho.gpt.custom.realtime.RealtimeProtocol;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

final class LiveFixture implements RealtimeProtocol.Events {
    static final String MODEL = "gpt-6-luna";
    static final String TYPE = "type";
    final ArrayDeque<JSONObject> queue = new ArrayDeque<>();
    final List<JSONObject> sent = new ArrayList<>();
    @Override public void send(JSONObject event, RequestScope scope) throws Exception { scope.check(); sent.add(event); }
    @Override public JSONObject receive(RequestScope scope) throws Exception { scope.check(); return queue.remove(); }
    void ready() throws Exception {
        queue.add(event("session.started").put("session", LiveSession.start(MODEL, SpeechApiConfig.DEFAULT, request(new Conversation())).getJSONObject("session")));
    }
    void response(String status, JSONObject... items) throws Exception {
        queue.add(nested(event("response.created").put("response", new JSONObject().put("id", "r1"))));
        for (int index = 0; index < items.length; index++) {
            queue.add(nested(item("response.output_item.added", index, items[index])));
            queue.add(nested(item("response.output_item.done", index, items[index])));
        }
        queue.add(nested(event("response.completed").put("response", new JSONObject().put("id", "r1").put("status", status).put("output", new JSONArray()))));
    }
    static JSONObject item(String type, int index, JSONObject item) throws Exception {
        return event(type).put("response_id", "r1").put("output_index", index).put("item", item);
    }
    static JSONObject nested(JSONObject event) throws Exception { return event("response.event").put("delegation_id", "d1").put("event", event); }
    static JSONObject event(String type) throws Exception { return new JSONObject().put(TYPE, type); }
    static JSONObject request(Conversation history) throws Exception {
        return new JSONObject().put("instructions", "Help").put("input", history.input()).put("tools", new JSONArray());
    }
    static JSONObject call() throws Exception {
        return new JSONObject().put(TYPE, "function_call").put("id", "fc1").put("call_id", "c1").put("name", "find_intents")
                .put("arguments", "{}").put("status", "completed");
    }
    static JSONObject message() throws Exception {
        return new JSONObject().put(TYPE, "message").put("id", "m1").put("status", "completed").put("role", "assistant")
                .put("content", new JSONArray().put(new JSONObject().put(TYPE, "output_text").put("text", "Done")));
    }
}
