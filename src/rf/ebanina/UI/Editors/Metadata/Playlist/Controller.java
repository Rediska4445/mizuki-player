package rf.ebanina.UI.Editors.Metadata.Playlist;

import javafx.fxml.Initializable;
import rf.ebanina.ebanina.Player.Playlist;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private Playlist playlist;

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
