package aho.gpt.openai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Only a completed response is executable; partial calls must never run. */
public final class SseResponseReader {
    private SseResponseReader() { }

    public static JSONObject read(Reader source) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(source);
        StringBuilder data = new StringBuilder();
        Map<Integer, JSONObject> items = new TreeMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            checkCancelled();
            if (line.isEmpty()) {
                JSONObject completed = event(data, items);
                if (completed != null) { return completed; }
                data.setLength(0);
            } else { appendLine(data, line); }
        }
        JSONObject completed = event(data, items);
        if (completed != null) { return completed; }
        throw new IOException("Response ended before completion. No actions were executed.");
    }

    private static void checkCancelled() throws IOException {
        if (Thread.currentThread().isInterrupted()) { throw new IOException("Request cancelled"); }
    }

    private static void appendLine(StringBuilder data, String line) throws IOException {
        if (!line.startsWith("data:")) { return; }
        data.append(line.substring(5).trim()).append('\n');
        if (data.length() > 2_000_000) { throw new IOException("Response event too large"); }
    }

    private static JSONObject event(StringBuilder data, Map<Integer, JSONObject> items) throws JSONException, IOException {
        if (data.length() == 0) { return null; }
        if ("[DONE]".equals(data.toString().trim())) { return null; }
        JSONObject event = new JSONObject(data.toString());
        String type = event.optString("type");
        if ("response.completed".equals(type)) { return completed(event.getJSONObject("response"), items); }
        if ("response.output_item.done".equals(type)) {
            items.put(event.getInt("output_index"), event.getJSONObject("item"));
        }
        if (failed(type)) {
            throw new IOException("OpenAI could not complete the response: " + event.toString());
        }
        return null;
    }
    private static JSONObject completed(JSONObject response, Map<Integer, JSONObject> items) throws JSONException {
        JSONArray output = response.getJSONArray("output");
        for (var item : items.entrySet()) { output.put(item.getKey(), item.getValue()); }
        return response;
    }
    private static boolean failed(String type) {
        return "response.failed".equals(type) || "response.incomplete".equals(type) || "error".equals(type);
    }
}
