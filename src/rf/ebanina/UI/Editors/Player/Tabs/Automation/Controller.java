package rf.ebanina.UI.Editors.Player.Tabs.Automation;

import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.File.Localization.LocalizationManager;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    public Button addButton;
    public Button removeButton;
    public ListView<Automation> listView;

    private static final List<Automation> savedAutomations = new ArrayList<>();
    public Tab tab;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listView.getItems().setAll(savedAutomations);

        tab.setText(LocalizationManager.getLocaleString("vst_editor_tab_auto", "Auto"));

        addButton.setText(LocalizationManager.getLocaleString("vst_editor_add_auto", "add"));
        removeButton.setText(LocalizationManager.getLocaleString("vst_editor_remove_auto", "rem"));

        addButton.setOnAction((e) -> {
            Automation automation = new Automation(new Dimension(800, 50));

            savedAutomations.add(automation);
            listView.getItems().add(automation);

            MediaProcessor.mediaProcessor.onMediaPlayerCreate = (mediaPlayer) -> {
                MediaProcessor.mediaProcessor.mediaPlayerPrepare(mediaPlayer);

                automation.setParamRange("Громкость", 0.0, 1.0, mediaPlayer::setVolume);
                automation.setParamRange("Темп", 0.5, 2.0, value -> mediaPlayer.setTempo(value.floatValue()));
                automation.setParamRange("Панорама", -1.0, 1.0, value -> mediaPlayer.setPan(value.floatValue()));
                automation.setObserving(true);
                automation.bindToMediaPlayer(mediaPlayer);
            };

            automation.setParamRange("Громкость", 0.0, 1.0, value -> MediaProcessor.mediaProcessor.mediaPlayer.setVolume(value));
            automation.setParamRange("Панорама", -1.0, 1.0, value -> MediaProcessor.mediaProcessor.mediaPlayer.setPan(value.floatValue()));
            automation.setParamRange("Темп", 0.5, 2.0, value -> MediaProcessor.mediaProcessor.setTempo(value.floatValue()));
            automation.setObserving(true);
            automation.bindToMediaPlayer(MediaProcessor.mediaProcessor.mediaPlayer);
        });

        removeButton.setOnAction((e) -> {
            Automation selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                savedAutomations.remove(selected);
                listView.getItems().remove(selected);
            }
        });
    }
}
