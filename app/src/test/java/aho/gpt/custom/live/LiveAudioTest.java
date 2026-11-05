package aho.gpt.custom.live;

import aho.gpt.custom.realtime.WireBase64;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class LiveAudioTest {
    @Test public void silenceDoesNotProduceAnAnswerOrGrowTheBuffer() throws Exception {
        LiveAudio audio = new LiveAudio(); String silence = WireBase64.encode(new byte[96_000]);
        audio.append(silence); assertFalse(audio.settled()); assertEquals(0, audio.bytes().length);
        audio.append(WireBase64.encode(new byte[]{0, 20})); audio.append(silence);
        assertTrue(audio.settled()); int length = audio.bytes().length;
        audio.append(silence); assertEquals(length, audio.bytes().length);
        audio.backendFinished(); assertFalse(audio.settled());
        audio.clear(); assertEquals(0, audio.bytes().length);
    }
    @Test public void oddOrMalformedPcmCannotBePlayed() {
        LiveAudio audio = new LiveAudio();
        assertThrows(IOException.class, () -> audio.append(WireBase64.encode(new byte[1])));
        assertThrows(IOException.class, () -> audio.append("invalid"));
    }
}
