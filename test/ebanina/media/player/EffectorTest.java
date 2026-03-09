package ebanina.media.player;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import rf.ebanina.ebanina.Player.AudioEffect.Effector;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;
import rf.ebanina.File.Resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;

public class EffectorTest {

    private Effector effector;

    @BeforeAll
    static void setUpResourceManager(@TempDir Path tempDir) {
        ResourceManager.Instance = new ResourceManager("");
        ResourceManager.Instance.resourcesPaths = new java.util.HashMap<>();
        ResourceManager.Instance.resourcesPaths.put("pluginsPath", tempDir.resolve("plugins").toString());
    }

    @BeforeEach
    void setUp() {
        effector = Effector.instance;
    }

    @Test
    @DisplayName("load() должен вернуть null для несуществующего файла")
    void loadShouldReturnNullForNonExistentFile() {
        // Given
        File nonExistent = new File("non-existent.plugin");

        // When
        IAudioEffect result = effector.load(nonExistent);

        // Then
        assertNull(result, "Для несуществующего файла должен вернуться null");
    }

    @Test
    @DisplayName("load() должен вернуть null для пустого файла")
    void loadShouldReturnNullForEmptyFile() throws IOException {
        // Given
        File emptyFile = new File("empty.plugin");
        if (emptyFile.exists()) emptyFile.delete();
        emptyFile.createNewFile();

        // When
        IAudioEffect result = effector.load(emptyFile);

        // Then
        assertNull(result, "Для пустого файла должен вернуться null");
    }

    @Test
    @DisplayName("load() должен вернуть null для повреждённого .plugin файла")
    void loadShouldReturnNullForCorruptedFile() throws IOException {
        // Given
        File corrupted = new File("corrupted.plugin");
        if (corrupted.exists()) corrupted.delete();

        try (var out = new java.io.FileOutputStream(corrupted)) {
            out.write("I'm not a serialized object!".getBytes());
        }

        // When
        IAudioEffect result = effector.load(corrupted);

        // Then
        assertNull(result, "Для повреждённого файла должен вернуться null");
    }

    @AfterEach
    void tearDown() {
        // Очистка
        new File("valid_volumer.plugin").delete();
        new File("empty.plugin").delete();
        new File("corrupted.plugin").delete();
    }
}
