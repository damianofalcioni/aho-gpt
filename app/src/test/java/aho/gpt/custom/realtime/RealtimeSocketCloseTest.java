package aho.gpt.custom.realtime;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** TLS shutdown must never run on the thread closing the voice panel. */
public class RealtimeSocketCloseTest {
    @Test public void closeDetachesOnceAndDoesNotWaitForNetworkShutdown() throws Exception {
        try (BlockingSocket socket = new BlockingSocket(); RealtimeSocket client = client(socket)) {
            try {
                client.close();
                assertTrue(socket.started.await(2, TimeUnit.SECONDS));
                assertNotSame(Thread.currentThread(), socket.closingThread);
                client.close();
                assertEquals(1, socket.calls.get());
                assertEquals(1, socket.finished.getCount());
            } finally { socket.release.countDown(); }
            assertTrue(socket.finished.await(2, TimeUnit.SECONDS));
        }
    }
    @Test public void aSlowCloseDoesNotDelayAnotherCancelledSocket() throws Exception {
        try (BlockingSocket first = new BlockingSocket(); BlockingSocket second = new BlockingSocket();
                RealtimeSocket one = client(first); RealtimeSocket two = client(second)) {
            try {
                one.close();
                assertTrue(first.started.await(2, TimeUnit.SECONDS));
                two.close();
                assertTrue(second.started.await(2, TimeUnit.SECONDS));
            } finally { first.release.countDown(); second.release.countDown(); }
            assertTrue(first.finished.await(2, TimeUnit.SECONDS));
            assertTrue(second.finished.await(2, TimeUnit.SECONDS));
        }
    }
    private RealtimeSocket client(Socket socket) throws Exception {
        RealtimeSocket client = new RealtimeSocket();
        var field = RealtimeSocket.class.getDeclaredField("active");
        field.setAccessible(true); field.set(client, socket); return client;
    }
    private static final class BlockingSocket extends Socket {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch finished = new CountDownLatch(1);
        private final AtomicInteger calls = new AtomicInteger();
        private Thread closingThread;
        @Override public void close() throws IOException {
            if (calls.incrementAndGet() > 1) { return; }
            closingThread = Thread.currentThread(); started.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) { throw new IOException("Test shutdown timed out"); }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); throw new IOException(exception);
            } finally { finished.countDown(); }
        }
    }
}
