package aho.gpt;

import aho.gpt.core.AppLanguages;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class AppLanguagesTest {
    @Test public void matchesFirstSupportedSystemPreference() {
        assertEquals("it", AppLanguages.match("de-DE,it-IT,en-US"));
        assertEquals("en", AppLanguages.match("fr-FR,en-GB,it-IT"));
        assertEquals("it", AppLanguages.match("it-CH"));
    }
    @Test public void unsupportedSystemLanguagesFallBackToEnglish() {
        assertEquals("en", AppLanguages.match("de-DE,fr-FR"));
        assertEquals("en", AppLanguages.match(""));
    }
    @Test public void normalizesManualChoiceAndRejectsUnsupportedLanguages() {
        assertEquals("it", AppLanguages.supported("it-IT"));
        assertEquals("en", AppLanguages.supported("en-US"));
        assertEquals("en", AppLanguages.supported("fr-FR"));
        assertEquals("en", AppLanguages.supported(null));
    }
}
