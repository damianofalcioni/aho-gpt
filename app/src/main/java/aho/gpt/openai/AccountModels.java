package aho.gpt.openai;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Public ChatGPT plan catalog: keep visible choices in server order. */
public final class AccountModels {
    public record Model(String slug, String name) { }
    private AccountModels() { }
    public static List<Model> read(JSONObject response) throws JSONException {
        JSONArray models = response.getJSONArray("models");
        List<Model> result = new ArrayList<>();
        for (int i = 0; i < models.length(); i++) {
            JSONObject model = models.getJSONObject(i);
            if ("list".equals(model.optString("visibility"))) {
                result.add(new Model(model.getString("slug"), model.getString("display_name")));
            }
        }
        return result;
    }
}
