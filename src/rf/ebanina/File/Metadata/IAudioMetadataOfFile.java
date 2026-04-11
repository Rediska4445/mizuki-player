package rf.ebanina.File.Metadata;

import rf.ebanina.ebanina.Player.Track;

import java.awt.image.BufferedImage;

public interface IAudioMetadataOfFile
        extends IMetadata
{
    int getDuration(String path);
    int getAudioFileDuration(String path);

    String getTitle(String path);
    String getArtist(String path);
    javafx.scene.image.Image getArt(Track path, int size, int size1, boolean preserve_ration, boolean smooth);

    void setTitle(String path, String title);
    void setArtist(String path, String artist);
    void setArt(String path, BufferedImage image);
}