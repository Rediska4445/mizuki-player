package ebanina.io.metadata;

import org.junit.jupiter.api.Test;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Metadata.Formats.WAV;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WAVTest extends ebanina.Test {

    private final WAV wav = new WAV();

    @Test
    void getFormat_returnsWAV() {
        assertEquals("wav", wav.getFormat());
    }

    @Test
    void getTitle_extractsTECHNO_KILLA() {
        String title = wav.getTitle(guineaPigs("metadata" + File.separator + "wav.wav").toString());
        assertEquals("TECHNO KILLA", title);
    }

    @Test
    void getArtist_extractsKUTE() {
        String artist = wav.getArtist(guineaPigs("metadata" + File.separator + "wav.wav").toString());
        assertEquals("KUTE", artist);
    }

    @Test
    void getAudioFileDuration_returns130Seconds() {
        int duration = wav.getAudioFileDuration(guineaPigs("metadata" + File.separator + "wav.wav").toString());
        assertEquals(130, duration);
    }

    @Test
    void getDuration_returnsZeroByDefault() {
        int duration = wav.getDuration(guineaPigs("metadata" + File.separator + "wav.wav").toString());
        assertEquals(0, duration);
    }

    @Test
    void getArt_returnsNull_forWAV() {
        Track track = new Track(guineaPigs("metadata" + File.separator + "wav.wav").toString());
        var art = wav.getArt(track, 128, 128, true, true);
        assertNull(art);
    }

    @Test
    void setTitle_doesNotThrow() {
        assertDoesNotThrow(() ->
                wav.setTitle(guineaPigs("metadata" + File.separator + "wav.wav").toString(), "New Title"));
    }

    @Test
    void setArtist_doesNotThrow() {
        assertDoesNotThrow(() ->
                wav.setArtist(guineaPigs("metadata" + File.separator + "wav.wav").toString(), "New Artist"));
    }

    @Test
    void setArt_doesNotThrow() {
        assertDoesNotThrow(() ->
                wav.setArt(guineaPigs("metadata" + File.separator + "wav.wav").toString(),
                        new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)));
    }

    @Test
    void getMetadataValue_returnsNull_forUnknownKey() {
        String value = wav.getMetadataValue(guineaPigs("metadata" + File.separator + "wav.wav").toString(), "GENRE");
        assertNull(value);
    }

    @Test
    void setMetadataValue_doesNotThrow() {
        assertDoesNotThrow(() ->
                wav.setMetadataValue(guineaPigs("metadata" + File.separator + "wav.wav").toString(), "GENRE", "Techno"));
    }

    @Test
    void getAllMetadata_returnsEmptyList() {
        List<Map.Entry<String, String>> metadata = wav.getAllMetadata(guineaPigs("metadata" + File.separator + "wav.wav").toString());
        assertTrue(metadata.isEmpty());
    }

    // Негативные тесты

    @Test
    void nonWavFile_title_returnsNull() {
        String title = wav.getTitle(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        assertNull(title);
    }

    @Test
    void nonWavFile_artist_returnsNull() {
        String artist = wav.getArtist(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        assertNull(artist);
    }

    @Test
    void nonWavFile_duration_returnsZero() {
        int duration = wav.getAudioFileDuration(guineaPigs("metadata" + File.separator + "mp.mp3").toString());
        assertEquals(0, duration);
    }

    @Test
    void invalidPath_returnsNullOrZero() {
        String title = wav.getTitle("/nonexistent/file.wav");
        String artist = wav.getArtist("/nonexistent/file.wav");
        int duration = wav.getAudioFileDuration("/nonexistent/file.wav");

        assertNull(title);
        assertNull(artist);
        assertEquals(0, duration);
    }

    @Test
    void getTitle_fromINAM_only() {
        // Тест только на INAM (title), artist должен быть null
        String title = wav.getTitle(guineaPigs("metadata" + File.separator + "wav.wav").toString());
        assertEquals("TECHNO KILLA", title);
    }

    @Test
    void getArtist_fromIART_only() {
        // Тест только на IART (artist), title должен быть отдельно
        String artist = wav.getArtist(guineaPigs("metadata" + File.separator + "wav.wav").toString());
        assertEquals("KUTE", artist);
    }
}
