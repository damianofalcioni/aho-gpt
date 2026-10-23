package aho.gpt;

import aho.gpt.core.SpeechLanguageOptions;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpeechLanguageOptionsTest {
    @Test public void unknownProviderMayOfferUnverifiedDeviceLocales() {
        assertEquals(List.of("de-AT", "en-US"), SpeechLanguageOptions.choices(null,
                List.of(Locale.US, Locale.forLanguageTag("de-AT"))));
    }
    @Test public void explicitlyEmptyProviderNeverAdvertisesDeviceLocalesAsSupported() {
        assertTrue(SpeechLanguageOptions.choices(List.of(), List.of(Locale.US)).isEmpty());
    }
    @Test public void providerCatalogPreservesRegionalDifferencesAndRemovesMalformedEntries() {
        assertEquals(List.of("en-GB", "en-US"), SpeechLanguageOptions.choices(
                Arrays.asList("en_US", "EN-us", "en-GB", "en-@-US", "und", null), List.of(Locale.ITALY)));
    }
    @Test public void defaultAndInvalidSelectionsResolveToEmptyRatherThanAnInventedLanguage() {
        assertEquals("", SpeechLanguageOptions.normalize(null));
        assertEquals("", SpeechLanguageOptions.normalize("  "));
        assertEquals("", SpeechLanguageOptions.normalize("invalid-language-tag-!!!"));
        assertEquals("de-AT", SpeechLanguageOptions.normalize(" de_AT "));
    }
}
