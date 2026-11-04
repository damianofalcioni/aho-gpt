package aho.gpt.custom.realtime;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Refuses extensions, masking by a server, oversized frames and invalid lengths. */
final class WebSocketFrameReader {
    record Frame(boolean last, int opcode, byte[] body) { }
    private final DataInputStream input;
    WebSocketFrameReader(InputStream input) { this.input = new DataInputStream(input); }
    Frame read() throws IOException {
        int first = input.readUnsignedByte(); int second = input.readUnsignedByte();
        if ((first & 112) != 0 || (second & 128) != 0) { throw new IOException("Invalid WebSocket frame"); }
        int size = length(second & 127);
        if ((first & 15) >= 8 && size > 125) { throw new IOException("Oversized WebSocket control"); }
        byte[] body = new byte[size]; input.readFully(body);
        return new Frame((first & 128) != 0, first & 15, body);
    }
    private int length(int marker) throws IOException {
        long length = marker;
        if (marker == 126) { length = input.readUnsignedShort(); }
        else if (marker == 127) { length = input.readLong(); }
        if (length < 0 || length > 8_000_000) { throw new IOException("Realtime frame too large"); }
        canonical(marker, length);
        return (int) length;
    }
    private void canonical(int marker, long length) throws IOException {
        if (marker == 126 && length < 126) { throw new IOException("Noncanonical WebSocket length"); }
        if (marker == 127 && length <= 65535) { throw new IOException("Noncanonical WebSocket length"); }
    }
}
