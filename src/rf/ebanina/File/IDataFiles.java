package rf.ebanina.File;

import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public interface IDataFiles
{
    File getFileFromOpenFileDialog(Stage stage);

    boolean hasSupportedExtension(Path path);
    void setPlaylists(String pathMainFolder) throws IOException;

    ArrayList<Track> getMusic(Path path) throws IOException;
    ArrayList<Playlist> getPlaylists(String pathMainFolder) throws IOException;
    List<String> supportedExtensions();
}
