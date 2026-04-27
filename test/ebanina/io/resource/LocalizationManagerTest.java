package ebanina.io.resource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import rf.ebanina.File.Localization.LocalizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class LocalizationManagerTest {
    @Test
    void testGetLocalizationString_realResources() {
        LocalizationManager manager = Mockito.mock(LocalizationManager.class);

        String word = "lang";
        String defaultValue = "default_value_if_missing";

        when(manager.getLocalizationString(eq("lang"), eq("default_value_if_missing")))
                .thenReturn("Язык");

        String localized = manager.getLocalizationString(word, defaultValue).strip().trim();

        assertEquals("Язык", localized);
    }
}
