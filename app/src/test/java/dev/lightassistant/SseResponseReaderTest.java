package dev.lightassistant;

import dev.lightassistant.openai.SseResponseReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.*;

public class SseResponseReaderTest {
    @Test public void returnsOnlyCompletedResponse() throws Exception {
        String events = ": keepalive\n\nevent: response.output_text.delta\ndata: {\"type\":\"response.output_text.delta\",\"delta\":\"Hi\"}\n\n"
                + "data: {\"type\":\"response.completed\",\"response\":{\"output\":[]}}\n\n";
        assertEquals(0, SseResponseReader.read(new StringReader(events)).getJSONArray("output").length());
    }
    @Test public void rejectsTruncatedStreamWithPartialTool() throws Exception {
        String partial = "data: {\"type\":\"response.output_item.added\",\"item\":{\"type\":\"function_call\"}}\n\n";
        assertThrows(IOException.class, () -> SseResponseReader.read(new StringReader(partial)));
    }
    @Test public void rejectsLateFailureAfterText() {
        String events = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Done\"}\n\n"
                + "data: {\"type\":\"response.failed\",\"response\":{\"error\":{\"code\":\"subscription_sharing_usage_limit_exceeded\"}}}\n\n";
        assertThrows(IOException.class, () -> SseResponseReader.read(new StringReader(events)));
    }
    @Test public void rejectsIncompleteAndDoneWithoutCompletion() {
        assertThrows(IOException.class, () -> SseResponseReader.read(new StringReader("data: [DONE]\n\n")));
        assertThrows(IOException.class, () -> SseResponseReader.read(new StringReader("data: {\"type\":\"response.incomplete\"}\n\n")));
    }
    @Test public void replaysCompletedEncryptedReasoningItems() throws Exception {
        String events = "data: {\"type\":\"response.output_item.done\",\"output_index\":0,\"item\":{\"type\":\"reasoning\",\"encrypted_content\":\"opaque\"}}\n\n"
                + "data: {\"type\":\"response.completed\",\"response\":{\"output\":[{\"type\":\"reasoning\"}]}}\n\n";
        assertEquals("opaque", SseResponseReader.read(new StringReader(events)).getJSONArray("output")
                .getJSONObject(0).getString("encrypted_content"));
    }
}
