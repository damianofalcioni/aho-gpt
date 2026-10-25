package aho.gpt.core;

/** Selection callback shared by catalog and protocol code, including Android 6. */
public interface ToolSelection {
    boolean enabled(String key);
}
