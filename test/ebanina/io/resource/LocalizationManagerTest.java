package ebanina.io.resource;

import org.junit.jupiter.api.Test;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Localization.LocalizationManager;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

//TODO: Да в пизду этот mockito
public class LocalizationManagerTest {
    @Test
    void testGetLocalizationString_realResources() {
        LocalizationManager manager = new LocalizationManager(new FileManager(""), "RU_ru", Path.of("test-res", "lang", "RU_ru.locale"));

        String word = "lang";
        String defaultValue = "default_value_if_missing";

        String localized = manager.getLocalizationString(word, defaultValue).strip().trim();

        assertEquals("Язык", localized);
    }
}
