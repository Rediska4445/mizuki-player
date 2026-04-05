package rf.ebanina.File.Metadata;

import rf.ebanina.ebanina.Player.Track;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public interface IAudioMetadataOfFile
{
    int getDuration(String path);
    String getTitle(String path);
    String getArtist(String path);
    int getAudioFileDuration(String path);
    javafx.scene.image.Image getArt(Track path, int size, int size1, boolean preserve_ration, boolean smooth);

    void setTitle(String path, String title);
    void setArtist(String path, String artist);
    void setArt(String path, BufferedImage image);

    String getMetadataValue(String path, String key);
    void setMetadataValue(String path, String key, String value);

    List<Map.Entry<String, String>> getAllMetadata(String path);
}