package ebanina.io.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.FileManager;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ConfigurationManagerTest {

    private ConfigurationManager configManager;

    // Мок FileManager
    @Mock
    private FileManager fileManagerMock;

    private Path testSettingsPath = Paths.get("src/test/resources/testConfig.properties");
    private Path testHotKeysPath = Paths.get("src/test/resources/hotkeys/");

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        configManager = new ConfigurationManager(testSettingsPath, testHotKeysPath, Path.of("config"));

        FileManager.instance = fileManagerMock;

        when(fileManagerMock.splitData(anyString())).thenAnswer(invocation -> {
            String data = invocation.getArgument(0);
            if (data.contains("=") && data.contains(";")) {
                String trim1 = data.replace(" ", "");
                return trim1.substring(trim1.lastIndexOf("=") + 1, trim1.indexOf(";"));
            }

            return data;
        });
    }

    @Test
    public void testGetItemString() {
        String key = "app.name";
        String expectedValue = "MyApp";
        when(fileManagerMock.findFirstParam(eq(key), eq(testSettingsPath), anyString())).thenReturn(expectedValue);

        String actual = configManager.getItem(key, "DefaultApp");
        assertEquals(expectedValue, actual);

        // Проверяем, что значение кешируется
        assertEquals(expectedValue, configManager.getConfigurationMap().get(key));
    }

    @Test
    public void testGetItemPrimitive() {
        String key = "max.connections";
        String valueFromFile = "100";
        when(fileManagerMock.findFirstParam(eq(key), eq(testSettingsPath), anyString())).thenReturn(valueFromFile);

        int actualInt = configManager.getItem(Integer.class, key, 50);
        assertEquals(100, actualInt);

        boolean actualBool;
        when(fileManagerMock.findFirstParam(eq("feature.enabled"), eq(testSettingsPath), anyString())).thenReturn("true");
        // Нужно переопределять мок прежде чем вызывать метод
        actualBool = configManager.getItem(Boolean.class, "feature.enabled", false);
        assertTrue(actualBool);
    }
}
