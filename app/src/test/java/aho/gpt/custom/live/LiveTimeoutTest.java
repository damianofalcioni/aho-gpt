package aho.gpt.custom.live;

import aho.gpt.core.RequestScope;
import aho.gpt.custom.realtime.WireBase64;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static aho.gpt.custom.live.LiveFixture.event;
import static org.junit.Assert.*;

public class LiveTimeoutTest {
    private static final String AUDIO = "session.output_audio.delta";
    private static final String DELTA = "delta";
    @Test public void arrivingSilenceCannotKeepAStalledRequestAlive() throws Exception {
        AtomicLong clock = new AtomicLong(); LiveFixture fixture = new LiveFixture();
        LiveEvents events = new LiveEvents(fixture, clock::get); RequestScope scope = new RequestScope();
        String silence = WireBase64.encode(new byte[4800]);
        fixture.queue.add(event(AUDIO).put(DELTA, silence));
        clock.set(59_000_000_000L); events.receive(scope);
        fixture.queue.add(event(AUDIO).put(DELTA, silence));
        clock.set(60_000_000_001L);
        assertThrows(LiveInactivityException.class, () -> events.receive(scope));
        assertTrue(events.diagnostics().contains("inputTranscript=false outputSpeech=false outputSettled=false progressIdleMs=60000"));
    }
    @Test public void transcriptionProgressStartsANewInactivityWindow() throws Exception {
        AtomicLong clock = new AtomicLong(); LiveFixture fixture = new LiveFixture();
        LiveEvents events = new LiveEvents(fixture, clock::get); RequestScope scope = new RequestScope();
        fixture.queue.add(event("session.input_transcript.delta").put(DELTA, "A request"));
        clock.set(59_000_000_000L); events.receive(scope);
        String silence = WireBase64.encode(new byte[4800]);
        fixture.queue.add(event(AUDIO).put(DELTA, silence));
        clock.set(118_000_000_000L); events.receive(scope);
        fixture.queue.add(event(AUDIO).put(DELTA, silence));
        clock.set(119_000_000_001L);
        assertThrows(LiveInactivityException.class, () -> events.receive(scope));
        assertTrue(events.diagnostics().contains("inputTranscript=true"));
        assertFalse(events.diagnostics().contains("A request"));
    }
}
