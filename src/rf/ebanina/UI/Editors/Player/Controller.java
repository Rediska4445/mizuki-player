package rf.ebanina.UI.Editors.Player;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.File.Resources.ResourceManager;

import java.net.URL;
import java.util.ResourceBundle;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;

public class Controller implements Initializable {
    public TabPane tabPane;
    public Label main_text;

    @FXML
    public VBox background;

    public static void updateMediaPlayerPlugins() {
        if (MediaProcessor.mediaProcessor.mediaPlayer != null) {
            MediaProcessor.mediaProcessor.mediaPlayer.setPlugins(AudioHost.instance.vstPlugins);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        main_text.setText(getLocaleString("vst_editor_main_text", "Audio-Host"));

        tabPane.getStylesheets().add(ResourceManager.Instance.loadStylesheet("tabpane"));
    }
}
