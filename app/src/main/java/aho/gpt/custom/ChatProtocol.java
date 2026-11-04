package aho.gpt.custom;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Complete non-streamed replies share the existing tool validation and cancellation policy. */
public final class ChatProtocol {
    private static final String FUNCTION = "function";
    private static final String TYPE = "type";
    private ChatProtocol() { }
    public static JSONObject request(JSONObject response, String model) throws JSONException {
        JSONArray tools = new JSONArray();
        JSONArray namespaces = response.getJSONArray("tools");
        for (int i = 0; i < namespaces.length(); i++) {
            JSONArray functions = namespaces.getJSONObject(i).getJSONArray("tools");
            for (int j = 0; j < functions.length(); j++) {
                JSONObject function = new JSONObject(functions.getJSONObject(j).toString());
                function.remove(TYPE);
                tools.put(new JSONObject().put(TYPE, FUNCTION).put(FUNCTION, function));
            }
        }
        JSONObject request = new JSONObject().put("model", model).put("stream", false)
                .put("messages", ChatMessages.create(response.getString("instructions"), response.getJSONArray("input")));
        if (tools.length() > 0) { request.put("tools", tools).put("parallel_tool_calls", false); }
        return request;
    }
    public static JSONObject response(JSONObject reply) throws JSONException, IOException {
        JSONObject choice = reply.getJSONArray("choices").getJSONObject(0);
        String reason = choice.getString("finish_reason");
        if (!"stop".equals(reason) && !"tool_calls".equals(reason)) { throw new IOException("Incomplete custom response"); }
        JSONObject message = choice.getJSONObject("message");
        JSONArray output = new JSONArray();
        String text = message.optString("content", "");
        if (!text.isEmpty()) {
            output.put(new JSONObject().put(TYPE, "message").put("role", "assistant").put("content", new JSONArray()
                    .put(new JSONObject().put(TYPE, "output_text").put("text", text))));
        }
        appendCalls(output, message.optJSONArray("tool_calls"));
        return new JSONObject().put("output", output);
    }
    private static void appendCalls(JSONArray output, JSONArray calls) throws JSONException, IOException {
        if (calls == null) { return; }
        for (int i = 0; i < calls.length(); i++) {
            JSONObject call = calls.getJSONObject(i);
            if (!FUNCTION.equals(call.getString(TYPE))) { throw new IOException("Unsupported custom tool"); }
            JSONObject function = call.getJSONObject(FUNCTION);
            output.put(new JSONObject().put(TYPE, "function_call").put("namespace", "device")
                    .put("call_id", call.getString("id")).put("name", function.getString("name"))
                    .put("arguments", function.getString("arguments")));
        }
    }
}
