package ebanina.io.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Metadata.Formats.MP3;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MP3Test extends ebanina.Test {
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
        assertTrue(duration > 100 && duration <= 150);
    }

    @Test
    void getArt_returnsDefaultLogo_forNoEmbeddedArt() {
        Track track = new Track(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        // Избегаем JavaFX: проверяем отсутствие исключений

        assertDoesNotThrow(() -> {
            var art = mp3.getArt(track, 128, 128, true, true);
            assertNotNull(art); // ResourceManager возвращает logo
        });
    }

    @Test
    @Timeout(1000)
    void setTitle_writesID3v2_andSaves() throws IOException {
        Path tempMp3 = guineaPigs("metadata" + File.separator + "mp.mp3");

        // Сохраняем исходное название перед тестом
        MP3 originalMp3 = new MP3();
        String originalTitle = originalMp3.getTitle(tempMp3.toString());

        // Выполняем тест
        mp3.setTitle(tempMp3.toString(), "NEW TITLE TEST");

        // Проверяем результат
        MP3 freshMp3 = new MP3();
        String newTitle = freshMp3.getTitle(tempMp3.toString());
        assertEquals("NEW TITLE TEST", newTitle);

        // Постусловие: восстанавливаем исходное название трека
        mp3.setTitle(tempMp3.toString(), originalTitle);

        // Проверяем восстановление
        MP3 restoredMp3 = new MP3();
        assertEquals(originalTitle, restoredMp3.getTitle(tempMp3.toString()));
    }

    @Test
    @Timeout(10)
    void setArtist_writesID3v2_andSaves() throws IOException {
        Path tempMp3 = guineaPigs("metadata" + File.separator + "mp.mp3");

        // Сохраняем исходное имя исполнителя
        String originalArtist = MetadataOfFileTest.ORIGINAL_METADATA_TEST_RESOURCE_ARTIST_NAME;

        // Выполняем тест
        mp3.setArtist(tempMp3.toString(), "NEW ARTIST TEST");

        // Проверяем результат
        MP3 freshMp3 = new MP3();
        String newArtist = freshMp3.getArtist(tempMp3.toString());
        assertEquals("NEW ARTIST TEST", newArtist);

        // Постусловие: восстанавливаем исходное имя исполнителя (KUTE)
        mp3.setArtist(tempMp3.toString(), originalArtist);

        // Проверяем восстановление
        MP3 restoredMp3 = new MP3();
        assertEquals("KUTE", restoredMp3.getArtist(tempMp3.toString()));
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
