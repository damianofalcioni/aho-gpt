package aho.gpt;

import aho.gpt.openai.ToolDefinitions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolDefinitionsTest {
    @Test public void discoveryAdvertisesOverviewAndOptionalStructuredFilters() throws Exception {
        JSONArray functions = ToolDefinitions.create(false).getJSONObject(0).getJSONArray("tools");
        assertEquals(2, functions.length());
        JSONObject discovery = functions.getJSONObject(0);
        assertEquals("find_intents", discovery.getString("name"));
        JSONObject parameters = discovery.getJSONObject("parameters");
        assertEquals(0, parameters.getJSONArray("required").length());
        assertFalse(parameters.getBoolean("additionalProperties"));
        JSONObject properties = parameters.getJSONObject("properties");
        assertTrue(properties.getJSONObject("mode").getJSONArray("enum").toString().contains("overview"));
        assertTrue(properties.getJSONObject("category").getJSONArray("enum").toString().contains("open_link"));
        assertTrue(properties.has("app")); assertTrue(properties.has("scheme")); assertTrue(properties.has("mime_type"));
        assertEquals(4, ToolDefinitions.create(true).getJSONObject(0).getJSONArray("tools").length());
    }
}
