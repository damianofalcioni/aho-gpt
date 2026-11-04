package aho.gpt.custom.realtime;

import aho.gpt.core.RequestScope;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import aho.gpt.custom.realtime.WebSocketFrameReader.Frame;

/** Masked client frames, fragmented UTF-8 messages, ping/pong and bounded reads. */
final class WebSocketFrames {
    private static final int LIMIT = 8_000_000;
    private final WebSocketFrameReader reader;
    private final WebSocketFrameWriter writer;
    WebSocketFrames(InputStream input, OutputStream output) { reader = new WebSocketFrameReader(input); writer = new WebSocketFrameWriter(output); }
    synchronized void text(String text) throws IOException {
        writer.send(1, text.getBytes(StandardCharsets.UTF_8));
    }
    String text(RequestScope scope) throws Exception {
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        boolean started = false;
        while (true) {
            Frame frame = dataFrame(scope);
            if (frame.opcode() != (started ? 0 : 1)) { throw new IOException("Invalid WebSocket message"); }
            started = true;
            append(message, frame.body());
            if (frame.last()) { return utf8(message.toByteArray()); }
        }
    }
    private Frame dataFrame(RequestScope scope) throws Exception {
        while (true) {
            scope.check(); Frame frame = reader.read();
            if (frame.opcode() < 8) { return frame; }
            control(frame);
        }
    }
    private void append(ByteArrayOutputStream message, byte[] body) throws IOException {
        if (message.size() + body.length > LIMIT) { throw new IOException("Realtime message too large"); }
        message.write(body);
    }
    private void control(Frame frame) throws IOException {
        validateControl(frame);
        switch (frame.opcode()) {
            case 8: throw new IOException("Realtime connection closed");
            case 9: writer.send(10, frame.body()); break;
            case 10: break;
            default: throw new IOException("Unknown WebSocket control");
        }
    }
    private void validateControl(Frame frame) throws IOException {
        if (!frame.last() || frame.body().length > 125) { throw new IOException("Invalid WebSocket control"); }
    }
    private String utf8(byte[] bytes) throws Exception {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
}
