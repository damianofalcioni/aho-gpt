package aho.gpt.core;

/** Foreground-only voice turns. Answer completion returns directly to listening. */
public final class VoiceLifecycle {
    private enum State { INACTIVE, LISTENING, WORKING, PLAYING, FINISHED, CLOSED }
    private State state = State.INACTIVE;
    private boolean foreground;

    /** Returns whether an active recognizer must be stopped on backgrounding. */
    public boolean foreground(boolean visible) {
        boolean wasListening = listening();
        foreground = visible;
        return wasListening && !visible;
    }
    public boolean visible() { return foreground; }
    public boolean closed() { return state == State.CLOSED; }
    public boolean listening() { return foreground && state == State.LISTENING; }

    public boolean requestListening() {
        if (state == State.CLOSED || state == State.WORKING) { return false; }
        state = State.LISTENING;
        return listening();
    }
    public boolean recognized() {
        if (!listening()) { return false; }
        state = State.WORKING;
        return true;
    }
    public boolean answered() {
        if (state != State.WORKING) { return false; }
        state = State.PLAYING;
        return true;
    }
    public boolean playbackCompleted() {
        if (state != State.PLAYING) { return false; }
        state = State.LISTENING;
        return true;
    }
    public void finished() { if (!closed()) { state = State.FINISHED; } }
    public boolean close() {
        if (closed()) { return false; }
        state = State.CLOSED;
        return true;
    }
}
