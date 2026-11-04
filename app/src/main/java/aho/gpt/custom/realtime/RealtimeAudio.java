package aho.gpt.custom.realtime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.json.JSONArray;
import org.json.JSONObject;

/** Converts the app's mono 16 kHz WAV capture into Realtime 24 kHz PCM input. */
public final class RealtimeAudio {
    private RealtimeAudio() { }
    public static JSONObject message(byte[] wav) throws Exception {
        byte[] pcm = pcm(wav);
        JSONObject part = new JSONObject().put("type", "input_audio").put("audio", WireBase64.encode(pcm));
        return new JSONObject().put("type", "message").put("role", "user").put("content", new JSONArray().put(part));
    }
    public static byte[] pcm(byte[] wav) throws IOException {
        if (wav.length < 46 || wav.length % 2 != 0) { throw new IOException("Invalid captured audio"); }
        ByteBuffer source = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        validate(source);
        int frames = (wav.length - 44) / 2;
        ByteBuffer output = ByteBuffer.allocate(frames * 3).order(ByteOrder.LITTLE_ENDIAN);
        for (int frame = 0; frame < frames * 3 / 2; frame++) {
            double position = frame * 2.0 / 3.0; int left = (int) position;
            short first = source.getShort(44 + left * 2);
            short second = source.getShort(44 + Math.min(left + 1, frames - 1) * 2);
            output.putShort((short) Math.round(first + (second - first) * (position - left)));
        }
        byte[] pcm = new byte[output.position()]; output.flip(); output.get(pcm); return pcm;
    }
    private static void validate(ByteBuffer source) throws IOException {
        if (source.getInt(24) != 16_000 || source.getShort(22) != 1 || source.getShort(34) != 16) {
            throw new IOException("Invalid capture format");
        }
    }
}
