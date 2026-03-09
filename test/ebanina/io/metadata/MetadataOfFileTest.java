package ebanina.io.metadata;

import org.junit.jupiter.api.Test;
import rf.ebanina.File.Metadata.Formats.MP3;
import rf.ebanina.File.Metadata.Formats.WAV;
import rf.ebanina.File.Metadata.MetadataOfFile;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetadataOfFileTest extends ebanina.Test {
    protected static final String ORIGINAL_METADATA_TEST_RESOURCE_ARTIST_NAME = "KUTE";
    protected static final String ORIGINAL_METADATA_TEST_RESOURCE_TITLE_NAME = "TECHNO KILLA";

    private final MetadataOfFile metadata = new MetadataOfFile(
            new HashMap<>(Map.of(
                    "mp3", new MP3(),
                    "wav", new WAV()
            ))
    );

    @Test
    void wav_metadata_title_artist_duration() {
        Path path = guineaPigs("metadata" + File.separator + "wav.wav");

        String title = metadata.getTitle(path.toString());
        String artist = metadata.getArtist(path.toString());
        int duration = metadata.getAudioFileDuration(path.toString());

        assertEquals("TECHNO KILLA", title);
        assertEquals("KUTE", artist);
        assertEquals(130, duration);
    }

    @Test
    void mp3_metadata_title_artist_duration() {
        Path path = guineaPigs("metadata" + File.separator + "mp.mp3");

        String title = metadata.getTitle(path.toString());
        String artist = metadata.getArtist(path.toString());
        int duration = metadata.getAudioFileDuration(path.toString());

        assertEquals("TECHNO KILLA", title);
        assertEquals("KUTE", artist);
        assertEquals(130, duration);
    }

    @Test
    void unsupported_extension_returns_defaults() {
        Path path = guineaPigs("metadata" + File.separator + "wav.wav");

        // подменяем карту форматов так, чтобы не было нужного расширения
        MetadataOfFile empty = new MetadataOfFile(new HashMap<>());

        String title = empty.getTitle(path.toString());
        String artist = empty.getArtist(path.toString());
        int duration = empty.getAudioFileDuration(path.toString());

        assertEquals(empty.unkTitle, title);
        assertEquals(empty.unkAuthor, artist);
        assertEquals(0, duration);
    }
}
