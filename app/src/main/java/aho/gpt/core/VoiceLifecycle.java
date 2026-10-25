package aho.gpt.core;

/** Foreground-only voice turns. Answer completion returns directly to listening. */
public final class VoiceLifecycle {
    public enum State { INACTIVE, LISTENING, THINKING, EXECUTING_TOOL, PLAYING, FINISHED, CLOSED }
    private State state = State.INACTIVE;
    private boolean foreground;

    /** Returns whether an active recognizer must be stopped on backgrounding. */
    public boolean foreground(boolean visible) {
        boolean wasListening = listening();
        foreground = visible;
        return wasListening && !visible;
    }
    public boolean visible() { return foreground; }
    public State state() { return state; }
    public boolean closed() { return state == State.CLOSED; }
    public boolean listening() { return foreground && state == State.LISTENING; }

    public boolean requestListening() {
        if (closed() || working()) { return false; }
        state = State.LISTENING;
        return listening();
    }
    public boolean recognized() {
        if (!listening()) { return false; }
        state = State.THINKING;
        return true;
    }
    public boolean working() { return state == State.THINKING || state == State.EXECUTING_TOOL; }
    public boolean progress(boolean executingTool) {
        if (!working()) { return false; }
        state = executingTool ? State.EXECUTING_TOOL : State.THINKING;
        return true;
    }
    public boolean answered() {
        if (state != State.THINKING) { return false; }
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
