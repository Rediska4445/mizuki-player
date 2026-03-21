package rf.ebanina.UI.Editors.Player.Tabs.AudioEffects;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.paint.Color;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AudioHost.PluginListCell;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;

public class Controller {
    public Tab tab;
    @FXML
    private ListView<IAudioEffect> ownPluginsListView;

    @FXML
    public void initialize() {
        ownPluginsListView.setCellFactory(e -> new PluginListCell<>());

        tab.setText(LocalizationManager.getLocaleString("vst_editor_tab_own_plugins", "Plugins"));

        for (IAudioEffect plugin : MediaProcessor.mediaProcessor.mediaPlayer.getAudioPlugins()) {
            ownPluginsListView.getItems().add(plugin);
        }

        Color clr = ColorProcessor.core.getMainClr();
        String hex = String.format("#%02X%02X%02X",
                (int) (clr.getRed() * 255),
                (int) (clr.getGreen() * 255),
                (int) (clr.getBlue() * 255));

        ownPluginsListView.setStyle(
                "-fx-border-color: " + hex + ";" +
                        "-fx-control-inner-background: #1E1E1E;" +
                        "-fx-background-color: #1E1E1E;"
        );
    }
}