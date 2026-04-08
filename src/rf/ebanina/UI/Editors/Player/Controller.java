package rf.ebanina.UI.Editors.Player;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;

import java.net.URL;
import java.util.ResourceBundle;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;

public class Controller
        implements Initializable
{
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
        Color mainColor = ColorProcessor.core.getMainClr();
        String hexColor = ColorProcessor.core.toHex(mainColor);

        main_text.setText(getLocaleString("vst_editor_main_text", "Audio-Host"));
        main_text.setStyle("-fx-font-weight: bold; -fx-text-fill: " + hexColor + ";");
        tabPane.getStylesheets().add(ResourceManager.Instance.loadStylesheet("tabpane"));
        tabPane.setStyle("-fx-accent-color: " + hexColor + ";");
    }
}
