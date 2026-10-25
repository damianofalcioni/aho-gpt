package aho.gpt;

import aho.gpt.core.CapabilityIdentity;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class CapabilityIdentityTest {
    @Test public void rescansLabelsAndFilterOrderingKeepUserChoices() throws Exception {
        JSONObject before = declaration();
        JSONObject after = declaration().put("id", "new-scan-id").put("app", "Renamed app").put("description", "New text");
        after.put("schemes", new org.json.JSONArray("[\"https\",\"http\"]"));
        after.put("authorities", new org.json.JSONArray("[{\"port\":443,\"host\":\"example.org\"}]"));
        assertEquals(CapabilityIdentity.key(before), CapabilityIdentity.key(after));
    }
    @Test public void differentComponentsActionsAndFiltersHaveSeparateChoices() throws Exception {
        String key = CapabilityIdentity.key(declaration());
        assertNotEquals(key, CapabilityIdentity.key(declaration().put("action", "custom.ACTION")));
        assertNotEquals(key, CapabilityIdentity.key(declaration().put("kind", "service")));
        assertNotEquals(key, CapabilityIdentity.key(declaration().put("component", "example/Other")));
        assertNotEquals(key, CapabilityIdentity.key(declaration().put("schemes", new org.json.JSONArray("[\"custom\"]"))));
        assertNotEquals(key, CapabilityIdentity.key(declaration().put("authorities", new org.json.JSONArray("[{\"host\":\"other.org\",\"port\":443}]"))));
    }
    private JSONObject declaration() throws Exception {
        return new JSONObject("""
                {"id":"scan-id","app":"Example","kind":"activity","component":"example/Main",
                 "action":"android.intent.action.VIEW","schemes":["http","https"],"mime_types":[],
                 "authorities":[{"host":"example.org","port":443}],"paths":[],"categories":[]}
                """);
    }
}
