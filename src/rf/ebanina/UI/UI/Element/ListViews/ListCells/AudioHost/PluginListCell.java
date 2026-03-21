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
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.AudioEffect.Effector;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;

public class PluginListCell<T>
        extends ListCell<IAudioEffect>
{
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
            name.setMinWidth(220);

            Color mainClr = ColorProcessor.core.getMainClr();
            String hex = String.format("#%02X%02X%02X",
                    (int) (mainClr.getRed() * 255),
                    (int) (mainClr.getGreen() * 255),
                    (int) (mainClr.getBlue() * 255));

            name.setText(item.getName());
            name.setTextFill(mainClr);

            String btnStyle = "-fx-background-color: #252525; " +
                    "-fx-border-color: " + hex + "; " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 4; " +
                    "-fx-border-radius: 4; " +
                    "-fx-cursor: hand;";

            openGuiBtn.setStyle(btnStyle);
            up.setStyle(btnStyle);
            down.setStyle(btnStyle);

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
