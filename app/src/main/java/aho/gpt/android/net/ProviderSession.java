package aho.gpt.android.net;

import aho.gpt.android.AssistantApplication;
import aho.gpt.core.RequestScope;
import aho.gpt.custom.ChatProtocol;
import aho.gpt.custom.ServerModels;
import org.json.JSONObject;

/** A spoken turn stays bound to its provider and configuration, including every tool round. */
public final class ProviderSession {
    private final AssistantApplication app;
    private final ProviderSettings settings;
    private final RequestScope scope;
    private final boolean custom;
    private final String revision;
    private String model;
    private String token;
    private String endpoint;
    public ProviderSession(AssistantApplication app, RequestScope scope) {
        this.app = app; this.scope = scope; settings = new ProviderSettings(app);
        revision = settings.revision(); custom = settings.customEnabled(); model = app.model();
    }
    public JSONObject respond(JSONObject body) throws Exception {
        check();
        JSONObject result;
        if (custom) {
            prepare();
            result = ChatProtocol.response(app.customHttp().json(endpoint + "/chat/completions", token,
                    ChatProtocol.request(body, model), scope));
        } else { result = app.http().response(app.auth().accessToken(), body, scope); }
        check(); return result;
    }
    private void prepare() throws Exception {
        if (endpoint != null) { return; }
        endpoint = settings.endpoint(); token = settings.token(); check();
        model = settings.customModel();
        if (model.isEmpty()) { model = ServerModels.first(app.customHttp().json(endpoint + "/models", token, null, scope)); }
        check();
    }
    private void check() throws InterruptedException {
        scope.check();
        if (!revision.equals(settings.revision()) || (!custom && !settings.chatgptEnabled())) {
            scope.cancel(); throw new InterruptedException("Provider changed");
        }
    }
}
