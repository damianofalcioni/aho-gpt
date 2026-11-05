package aho.gpt;

import aho.gpt.openai.ToolDefinitions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolDefinitionsTest {
    @Test public void disabledFunctionsAreNeverAdvertisedAndEmptyNamespaceIsOmitted() throws Exception {
        JSONArray selected = ToolDefinitions.create(true, name -> name.equals("read_screen"))
                .getJSONObject(0).getJSONArray("tools");
        assertEquals(1, selected.length()); assertEquals("read_screen", selected.getJSONObject(0).getString("name"));
        assertEquals(0, ToolDefinitions.create(true, name -> false).length());
        assertEquals(0, ToolDefinitions.create(false, name -> name.equals("read_screen")).length());
    }
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
        assertEquals("array", properties.getJSONObject("apps").getString("type"));
        assertEquals(4, ToolDefinitions.create(true).getJSONObject(0).getJSONArray("tools").length());
    }

    @Test public void invocationExplainsLimitsWithoutRepeatingThemInEverySearchResult() throws Exception {
        JSONObject invocation = ToolDefinitions.functions(false).getJSONObject(1);
        String description = invocation.getString("description");
        assertTrue(description.contains("Only string extras"));
        assertTrue(description.contains("File/content/nested intent URIs"));
        assertTrue(description.contains("URI grants"));
        assertTrue(description.contains("image streams"));
        assertTrue(description.contains("multiple-item lists"));
        assertTrue(description.contains("returned camera/document data"));
    }
}
