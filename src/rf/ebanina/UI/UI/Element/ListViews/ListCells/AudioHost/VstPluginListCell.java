package rf.ebanina.UI.UI.Element.ListViews.ListCells.AudioHost;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.UI.Editors.Player.AudioHost;
import rf.ebanina.UI.Editors.Player.Tabs.AudioPlugins.Vst.VstParamsWindow;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.UI.Root.showError;
import static rf.ebanina.UI.Editors.Player.Controller.updateMediaPlayerPlugins;

public class VstPluginListCell<T> extends ListCell<PluginWrapper> {
    private final Button openGuiBtn = new Button(getLocaleString("vst_editor_open_gui", "GUI"));
    private final Button reOpenGuiBtn = new Button(getLocaleString("vst_editor_re_open_gui", "Re-open GUI"));
    private final Button up = new Button("↑");
    private final Button down = new Button("↓");
    private final Button paramsBtn = new Button(getLocaleString("vst_editor_parameters", "Parameters"));

    private final Label type = new Label();
    private final Label name = new Label();
    private final Slider mixSlider = new Slider();

    private final HBox container = new HBox(4);

    public VstPluginListCell() {
        super();

        type.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14));
        type.setTextFill(Color.web("#4a6ca8"));

        name.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 14));
        name.setTextFill(Color.web("#4a6ca8"));
        name.setMinWidth(220);

        mixSlider.setMax(100);
        mixSlider.setMin(0);
        mixSlider.setPrefWidth(100);
        mixSlider.setShowTickMarks(false);
        mixSlider.setShowTickLabels(false);

        container.getChildren().addAll(type, openGuiBtn, reOpenGuiBtn, name, mixSlider, up, down, paramsBtn);

        openGuiBtn.setOnAction(e -> {
            PluginWrapper item = getItem();

            if (item != null) {
                try {
                    Platform.runLater(item::openEditor);
                } catch (Exception ex) {
                    showError("Error open GUI", ex.getMessage());
                }
            }
        });

        reOpenGuiBtn.setOnAction(e -> {
            PluginWrapper item = getItem();

            if (item != null) {
                try {
                    item.reOpenVst3GUI();
                } catch (Exception ex) {
                    showError("Ошибка пере-открытия GUI", ex.getMessage());
                }
            }
        });

        paramsBtn.setOnAction(e -> {
            PluginWrapper item = getItem();
            if (item != null) {
                new VstParamsWindow(item).show();
            }
        });

        up.setOnAction(e -> {
            PluginWrapper item = getItem();
            if (item != null) {
                moveRow(item, -1);
            }
        });

        down.setOnAction(e -> {
            PluginWrapper item = getItem();
            if (item != null) {
                moveRow(item, 1);
            }
        });

        mixSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            PluginWrapper item = getItem();
            if (item != null) {
                try {
                    item.setMix(newVal.intValue());
                } catch (Exception e) {
                    showError(e.getMessage(), e.getLocalizedMessage());
                }
            }
        });
    }

    @Override
    protected void updateItem(PluginWrapper item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            type.setText(item.getSdkVersion());
            name.setText(item.getVendorName() + " - " + item.getProductString());

            try {
                mixSlider.setValue(item.getMix());
            } catch (Exception e) {
                showError(e.getMessage(), e.getLocalizedMessage());
            }

            setText(null);
            setGraphic(container);
        }
    }

    private void moveRow(PluginWrapper row, int dir) {
        ListView<PluginWrapper> lv = getListView();
        if (lv == null)
            return;

        int idx = lv.getItems().indexOf(row);
        int newIdx = idx + dir;

        if (newIdx < 0 || newIdx >= lv.getItems().size())
            return;

        PluginWrapper ofDirPlugin = lv.getItems().get(newIdx);

        if (AudioHost.instance.vstPlugins != null) {
            AudioHost.instance.vstPlugins.set(idx, ofDirPlugin);
            AudioHost.instance.vstPlugins.set(newIdx, row);

            lv.getItems().set(idx, ofDirPlugin);
            lv.getItems().set(newIdx, row);

            updateMediaPlayerPlugins();
        }

        lv.refresh();
    }
}