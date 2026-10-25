package aho.gpt.openai;

import aho.gpt.core.Conversation;
import aho.gpt.core.RequestScope;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AgentLoop {
    public interface Transport { JSONObject respond(JSONObject request) throws Exception; }
    public interface Progress { void update(boolean executingTool); }
    public interface Tools {
        boolean accessibilityAvailable();
        default boolean enabled(String name) { return true; }
        default long configurationVersion() { return 0; }
        String execute(String name, JSONObject arguments) throws Exception;
    }
    private final Transport transport;
    private final Tools tools;
    private final Progress progress;

    public AgentLoop(Transport transport, Tools tools) { this(transport, tools, executing -> { }); }
    public AgentLoop(Transport transport, Tools tools, Progress progress) {
        this.transport = transport; this.tools = tools; this.progress = progress;
    }

    public String run(Conversation conversation, String model) throws Exception {
        return run(conversation, model, new RequestScope());
    }
    public String run(Conversation conversation, String model, RequestScope scope) throws Exception {
        long configuration = tools.configurationVersion();
        conversation.configureTools(configuration);
        ToolCalls calls = new ToolCalls(tools, scope);
        while (true) {
            scope.check();
            progress.update(false);
            JSONArray definitions = ToolDefinitions.create(tools.accessibilityAvailable(), tools::enabled);
            JSONObject request = new JSONObject().put("model", model).put("instructions", AgentInstructions.create(definitions))
                    .put("store", false).put("stream", true).put("input", conversation.input())
                    .put("include", new JSONArray().put("reasoning.encrypted_content"))
                    .put("tools", definitions).put("parallel_tool_calls", false);
            checkConfiguration(configuration, scope);
            JSONObject response = transport.respond(request);
            scope.check();
            checkConfiguration(configuration, scope);
            JSONArray output = response.getJSONArray("output");
            conversation.append(output);
            if (!executeCalls(conversation, output, calls)) { return ResponseText.extract(output); }
        }
    }

    private void checkConfiguration(long version, RequestScope scope) throws InterruptedException {
        if (version != tools.configurationVersion()) { scope.cancel(); throw new InterruptedException("Tool settings changed"); }
    }

    private boolean executeCalls(Conversation conversation, JSONArray output, ToolCalls calls) throws Exception {
        boolean called = false;
        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.getJSONObject(i);
            if ("function_call".equals(item.optString("type"))) {
                progress.update(true);
                conversation.toolResult(item.getString("call_id"), calls.execute(item));
                called = true;
            }
        }
        return called;
    }

}
