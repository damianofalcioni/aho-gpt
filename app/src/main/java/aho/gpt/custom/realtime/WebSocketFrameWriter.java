package aho.gpt.custom.realtime;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

/** Every outgoing frame uses a new unpredictable mask. */
final class WebSocketFrameWriter {
    private final DataOutputStream output;
    private final SecureRandom random = new SecureRandom();
    WebSocketFrameWriter(OutputStream output) { this.output = new DataOutputStream(output); }
    synchronized void send(int opcode, byte[] body) throws IOException {
        if (body.length > 8_000_000) { throw new IOException("Realtime message too large"); }
        output.writeByte(128 | opcode);
        if (body.length < 126) { output.writeByte(128 | body.length); }
        else if (body.length <= 65535) { output.writeByte(128 | 126); output.writeShort(body.length); }
        else { output.writeByte(128 | 127); output.writeLong(body.length); }
        byte[] mask = new byte[4]; random.nextBytes(mask); output.write(mask);
        byte[] masked = body.clone();
        for (int index = 0; index < masked.length; index++) { masked[index] ^= mask[index % 4]; }
        output.write(masked); output.flush();
    }
}
