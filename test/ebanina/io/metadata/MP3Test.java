package ebanina.io.metadata;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;
import rf.ebanina.File.Metadata.Formats.MP3;
import rf.ebanina.ebanina.Player.Track;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class MP3Test
        extends ebanina.Test
{
    @BeforeAll
    static void initJFX() {
        new JFXPanel();
    }

    private final MP3 mp3 = new MP3();

    @Test
    void getFormat_returnsMP3() {
        assertEquals("mp3", mp3.getFormat());
    }

    @Test
    void getTitle_fromID3v2() {
        String title = mp3.getTitle(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        assertEquals(MetadataOfFileTest.ORIGINAL_METADATA_TEST_RESOURCE_TITLE_NAME, title);
    }

    @Test
    void getArtist_fromID3() {
        String artist = mp3.getArtist(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        assertEquals(MetadataOfFileTest.ORIGINAL_METADATA_TEST_RESOURCE_ARTIST_NAME, artist);
    }

    @Test
    void getAudioFileDuration_mp3agic() {
        int duration = mp3.getAudioFileDuration(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        assertEquals(130, duration);
    }

    @Test
    void getDuration_usesMediaPlayer() {
        int duration = mp3.getDuration(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        // MediaPlayer.getOverDuration() должен вернуть ~130s
        assertTrue(duration > 5 && duration <= 255);
    }

    @Test
    void getArt_returnsDefaultLogo_forNoEmbeddedArt() {
        Track track = new Track(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        // Избегаем JavaFX: проверяем отсутствие исключений

        assertDoesNotThrow(() -> {
            var art = mp3.getArt(track, 128, 128, true, true);

            assertNotNull(art);
        });
    }

    @Test
    @Timeout(5000)
    void setTitle_writesID3v2_andSaves() throws IOException {
        Path original = guineaPigs("metadata" + File.separator + "mp.mp3");
        Path tempMp3 = guineaPigs("metadata" + File.separator + "mp-test-title.mp3");

        // Сбрасываем копию
        Files.deleteIfExists(tempMp3);
        Files.createFile(tempMp3);
        Files.copy(original, tempMp3, StandardCopyOption.REPLACE_EXISTING);

        // Сохраняем исходное название
        MP3 originalMp3 = new MP3();
        String originalTitle = originalMp3.getTitle(tempMp3.toString());

        try {
            mp3.setTitle(tempMp3.toString(), "NEW TITLE TEST");

            // Проверяем результат
            MP3 freshMp3 = new MP3();
            String newTitle = freshMp3.getTitle(tempMp3.toString());
            assertEquals("NEW TITLE TEST", newTitle);

            // Постусловие: восстановляем исходный заголовок
            mp3.setTitle(tempMp3.toString(), originalTitle);

            // Проверяем восстановление
            MP3 restoredMp3 = new MP3();
            assertEquals(originalTitle, restoredMp3.getTitle(tempMp3.toString()));
        } finally {
            // Убираем временный файл, даже если тест упадёт
            Files.deleteIfExists(tempMp3);
        }
    }

    @Test
    @Timeout(5000)
    void setArtist_writesID3v2_andSaves() throws IOException {
        Path original = guineaPigs("metadata" + File.separator + "mp.mp3");
        Path tempMp3 = guineaPigs("metadata" + File.separator + "mp-test.mp3");

        Files.deleteIfExists(tempMp3);
        Files.createFile(tempMp3);
        Files.copy(original, tempMp3, StandardCopyOption.REPLACE_EXISTING);

        String originalArtist = MetadataOfFileTest.ORIGINAL_METADATA_TEST_RESOURCE_ARTIST_NAME;

        try {
            mp3.setArtist(tempMp3.toString(), "NEW ARTIST TEST");

            MP3 freshMp3 = new MP3();
            String newArtist = freshMp3.getArtist(tempMp3.toString());
            assertEquals("NEW ARTIST TEST", newArtist);

            mp3.setArtist(tempMp3.toString(), originalArtist);

            MP3 restoredMp3 = new MP3();
            assertEquals("KUTE", restoredMp3.getArtist(tempMp3.toString()));
        } finally {
            Files.deleteIfExists(tempMp3);
        }
    }

    @Test
    void setArt_writesAlbumImage() throws IOException {
        Path tempMp3 = tempDir.resolve("test_set_art.mp3");
        Files.copy(guineaPigs("metadata" + File.separator + "mp.mp3"), tempMp3,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        mp3.setArt(tempMp3.toString(), testImage);

        // Проверяем наличие album art
        MP3 freshMp3 = new MP3();
        List<Map.Entry<String, String>> metadata = freshMp3.getAllMetadata(tempMp3.toString());
        boolean hasArt = metadata.stream().anyMatch(e -> "album_art_size".equals(e.getKey()));
        assertTrue(hasArt);
    }

    @Test
    void getMetadataValue_specificKeys() {
        Path mp3Path = guineaPigs("metadata" + File.separator + "mp.mp3");

        assertEquals("TECHNO KILLA", mp3.getMetadataValue(mp3Path.toString(), "title"));
        assertEquals("KUTE", mp3.getMetadataValue(mp3Path.toString(), "artist"));
        // Другие ключи могут быть null, если нет в теговом файле
        assertEquals("Techno", mp3.getMetadataValue(mp3Path.toString(), "genre"));
    }

    @Test
    void setMetadataValue_writesVariousTags() throws IOException {
        Path tempMp3 = tempDir.resolve("test_set_metadata.mp3");
        Files.copy(guineaPigs("metadata" + File.separator + "mp.mp3"), tempMp3,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        mp3.setMetadataValue(tempMp3.toString(), "genre", "Techno");
        mp3.setMetadataValue(tempMp3.toString(), "album", "Test Album");
        mp3.setMetadataValue(tempMp3.toString(), "year", "2024");

        MP3 freshMp3 = new MP3();
        assertEquals("Techno", freshMp3.getMetadataValue(tempMp3.toString(), "genre"));
        assertEquals("Test Album", freshMp3.getMetadataValue(tempMp3.toString(), "album"));
        assertEquals("2024", freshMp3.getMetadataValue(tempMp3.toString(), "year"));
    }

    @Test
    void getAllMetadata_containsExpectedFields() {
        List<Map.Entry<String, String>> metadata = mp3.getAllMetadata(
                guineaPigs("metadata" + File.separator + "mp.mp3").toString()
        );

        assertFalse(metadata.isEmpty());
        assertTrue(metadata.stream().anyMatch(e -> "title".equals(e.getKey())));
        assertTrue(metadata.stream().anyMatch(e -> "artist".equals(e.getKey())));
        assertTrue(metadata.stream().anyMatch(e -> "file_size_bytes".equals(e.getKey())));
    }

    @Test
    void invalidMp3_returnsGracefulDefaults() {
        String title = mp3.getTitle("/nonexistent.mp3");
        String artist = mp3.getArtist("/nonexistent.mp3");
        int duration = mp3.getAudioFileDuration("/nonexistent.mp3");

        assertNotNull(title); // filename fallback
        assertEquals("Unknown Artist", artist);
        assertEquals(0, duration);
    }

    @TempDir
    Path tempDir;
}
