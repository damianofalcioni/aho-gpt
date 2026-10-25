package aho.gpt;

import aho.gpt.core.RequestScope;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestScopeTest {
    @Test public void cancellationStopsAlreadyQueuedAction() {
        RequestScope scope = new RequestScope();
        int[] dispatches = {0};
        FutureTask<Integer> queued = new FutureTask<>(() -> scope.dispatch(() -> ++dispatches[0]));
        scope.cancel(); queued.run();
        ExecutionException failure = assertThrows(ExecutionException.class, queued::get);
        assertTrue(failure.getCause() instanceof InterruptedException);
        assertEquals(0, dispatches[0]);
    }
    @Test public void oldCancellationDoesNotRevokeNewRequest() throws Exception {
        RequestScope previous = new RequestScope();
        RequestScope current = new RequestScope();
        previous.cancel(); previous.cancel();
        assertEquals("current", current.dispatch(() -> "current"));
        assertThrows(InterruptedException.class, previous::check);
    }
    @Test public void interruptionStopsDispatch() {
        RequestScope scope = new RequestScope();
        Thread.currentThread().interrupt();
        try { assertThrows(InterruptedException.class, scope::check); }
        finally { Thread.interrupted(); }
    }
}
