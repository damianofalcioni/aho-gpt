package dev.lightassistant.android.speech;

import java.util.List;

/** Null means the provider did not report a catalog; empty means no languages are ready. */
public interface LanguageCatalogCallback {
    void loaded(List<String> languages);
}
