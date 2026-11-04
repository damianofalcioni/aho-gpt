package aho.gpt;

import aho.gpt.openai.AccountModels;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class AccountModelsTest {
    @Test public void publicPlanCatalogKeepsVisibleNamesAndServerOrder() throws Exception {
        var models = AccountModels.read(new JSONObject("""
                {"models":[
                  {"slug":"second","display_name":"Second","visibility":"list"},
                  {"slug":"hidden","display_name":"Hidden","visibility":"hidden"},
                  {"slug":"first","display_name":"First","visibility":"list"}
                ]}
                """));
        assertEquals(2, models.size());
        assertEquals(new AccountModels.Model("second", "Second"), models.get(0));
        assertEquals(new AccountModels.Model("first", "First"), models.get(1));
    }
    @Test(expected = JSONException.class) public void unrelatedApiCatalogIsNotAPlanCatalog() throws Exception {
        AccountModels.read(new JSONObject("{\"data\":[{\"id\":\"other-model\"}]}"));
    }
}
