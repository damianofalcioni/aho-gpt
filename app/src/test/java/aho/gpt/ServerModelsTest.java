package aho.gpt;

import aho.gpt.custom.ChatProtocol;
import aho.gpt.custom.ServerModels;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServerModelsTest {
    private static final String DATA = "data";
    private static final String ID = "id";
    private static final String SECOND = "second-model";
    private static final String FIRST = "first-model";
    @Test public void readsDistinctModelIdsInServerOrder() throws Exception {
        JSONObject catalog = new JSONObject().put(DATA, new JSONArray()
                .put(new JSONObject().put(ID, FIRST)).put(new JSONObject().put(ID, SECOND))
                .put(new JSONObject().put(ID, FIRST)));
        assertEquals(Arrays.asList(FIRST, SECOND), ServerModels.read(catalog));
        assertEquals(FIRST, ServerModels.first(catalog));
    }
    @Test public void skipsUnusableEntriesWithoutInventingModelNames() throws Exception {
        JSONObject catalog = new JSONObject().put(DATA, new JSONArray().put(JSONObject.NULL).put("bad")
                .put(new JSONObject()).put(new JSONObject().put(ID, 7))
                .put(new JSONObject().put(ID, "  ")).put(new JSONObject().put(ID, SECOND)));
        assertEquals(Collections.singletonList(SECOND), ServerModels.read(catalog));
        assertEquals(SECOND, ServerModels.first(catalog));
    }
    @Test public void emptyCatalogCannotSupplyAutomaticModel() throws Exception {
        JSONObject catalog = new JSONObject().put(DATA, new JSONArray());
        assertTrue(ServerModels.read(catalog).isEmpty());
        assertThrows(IOException.class, () -> ServerModels.first(catalog));
    }
    @Test public void refusesUnrelatedCatalogShapes() throws Exception {
        assertThrows(JSONException.class, () -> ServerModels.read(new JSONObject().put("models", new JSONArray())));
        assertThrows(JSONException.class, () -> ServerModels.read(new JSONObject().put(DATA, "bad")));
    }
    @Test public void completionUsesSelectedModelInsteadOfResponsesModel() throws Exception {
        JSONObject request = new JSONObject().put("model", "chatgpt-model").put("instructions", "Help")
                .put("input", new JSONArray()).put("tools", new JSONArray());
        assertEquals(SECOND, ChatProtocol.request(request, SECOND).getString("model"));
    }
}
