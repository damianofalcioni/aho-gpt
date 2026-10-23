package aho.gpt.openai;

import org.json.JSONObject;

/** Dispatch receipts never establish completion of a task inside another app. */
public final class ToolOutcome {
    private ToolOutcome() { }
    public static String refused(String code) throws Exception { return result("refused", "not_attempted", code); }
    public static String rejected(String code) throws Exception { return result("rejected", "attempted", code); }
    public static String accepted(String code) throws Exception { return result("accepted", "attempted", code); }
    public static String unknown(String code) throws Exception { return result("unknown", "unknown", code); }
    private static String result(String status, String dispatch, String code) throws Exception {
        return new JSONObject().put("status", status).put("dispatch", dispatch)
                .put("completion_verified", false).put("code", code).toString();
    }
}
