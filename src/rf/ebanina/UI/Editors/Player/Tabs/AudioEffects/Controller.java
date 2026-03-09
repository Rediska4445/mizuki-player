package rf.ebanina.UI.Editors.Player.Tabs.AudioEffects;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AudioHost.PluginListCell;

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
    }
}