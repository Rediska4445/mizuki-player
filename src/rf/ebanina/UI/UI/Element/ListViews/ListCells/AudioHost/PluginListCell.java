package rf.ebanina.UI.UI.Element.ListViews.ListCells.AudioHost;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;
import rf.ebanina.ebanina.Player.AudioEffect.Effector;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.UI.Root;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;

public class PluginListCell<T> extends ListCell<IAudioEffect> {
    @Override
    protected void updateItem(IAudioEffect item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            final Button openGuiBtn = new Button(getLocaleString("vst_editor_open_gui", "GUI"));

            final Button up = new Button("↑");
            final Button down = new Button("↓");
            final HBox container = new HBox(4);

            Label name = new Label(item.getName());
            name.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14));
            name.setTextFill(Color.web("#4a6ca8"));
            name.setMinWidth(220);

            openGuiBtn.setOnAction(e -> {
                try {
                    item.openEditor(new Stage());
                } catch (Exception ex) {
                    Root.showError("Ошибка открытия GUI", ex.getMessage());
                }
            });

            up.setOnAction(e -> moveRow(item, -1));
            down.setOnAction(e -> moveRow(item, 1));

            container.getChildren().clear();
            container.getChildren().add(openGuiBtn);
            container.getChildren().add(name);
            container.getChildren().add(up);
            container.getChildren().add(down);

            setGraphic(container);
        }
    }

    private void moveRow(IAudioEffect row, int dir) {
        ListView<IAudioEffect> lv = getListView();
        int idx = lv.getItems().indexOf(row);
        int newIdx = idx + dir;

        if (newIdx >= 0 && newIdx < lv.getItems().size()) {
            lv.getItems().remove(idx);
            lv.getItems().add(newIdx, row);

            Platform.runLater(() -> {
                Effector.instance.plugins.remove(idx);
                Effector.instance.plugins.add(newIdx, row);

                MediaProcessor.mediaProcessor.mediaPlayer.setAudioPlugins(Effector.instance.plugins);
            });
        }
    }
}
