package rf.ebanina.UI.Editors.Player.Tabs.AudioPlugins;

import com.jfoenix.controls.JFXButton;
import com.synthbot.audioplugin.vst.JVstLoadException;
import com.synthbot.audioplugin.vst.vst2.JVstHost24;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.UI.Editors.Player.AudioHost;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AudioHost.VstPluginListCell;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.AudioPlugins.IPluginWrapper;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST3;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST3LoadException;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.vst3;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static rf.ebanina.Network.Info.PlayersTypes.URI_NULL;
import static rf.ebanina.UI.Root.showError;

public class Controller {
    public Tab tab;

    @FXML
    public JFXButton serialize;
    @FXML
    public JFXButton deserialize;

    @FXML
    private Button addBtn;
    @FXML
    private Button removeBtn;
    @FXML
    private JFXButton removeAllPluginsBtn;
    @FXML
    private ListView<PluginWrapper> pluginListView;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        pluginListView.setCellFactory(e -> new VstPluginListCell<>());

        for (PluginWrapper plugin : MediaProcessor.mediaProcessor.mediaPlayer.getPlugins()) {
            pluginListView.getItems().add(plugin);
        }

        tab.setText(LocalizationManager.getLocaleString("vst_editor_tab_vst", "VST"));

        String hexColor = ColorProcessor.core.toHex(ColorProcessor.core.getMainClr());
        String bgDark = "#1E1E1E";

        pluginListView.setStyle(
                "-fx-background-color: " + bgDark + "; " +
                        "-fx-control-inner-background: " + bgDark + "; " +
                        "-fx-border-color: #333333; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8;"
        );

        String accentStyle = "-fx-background-color: " + hexColor + "; -fx-text-fill: white; -fx-background-radius: 6;";
        addBtn.setStyle(accentStyle);
        removeBtn.setStyle(accentStyle);
        removeAllPluginsBtn.setStyle(accentStyle);

        String ioBtnStyle = "-fx-background-color: #333333; -fx-border-color: " + hexColor + "; -fx-border-radius: 6; -fx-background-radius: 6;";
        serialize.setStyle(ioBtnStyle);
        deserialize.setStyle(ioBtnStyle);

        if (serialize.getGraphic() instanceof SVGPath) {
            ((SVGPath) serialize.getGraphic()).setFill(javafx.scene.paint.Color.web(hexColor));
        }
        if (deserialize.getGraphic() instanceof SVGPath) {
            ((SVGPath) deserialize.getGraphic()).setFill(javafx.scene.paint.Color.web(hexColor));
        }

        addBtn.setOnAction(e -> showFileChooserAndAddPlugin());
        removeBtn.setOnAction(e -> removeSelectedPlugin());
        removeAllPluginsBtn.setOnAction(e -> removeAllPlugins());
        serialize.setOnAction(e -> savePlugin());
        deserialize.setOnAction(e -> loadPlugin());
    }

    protected void loadPlugin() {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(stage);

        if(file != null) {
            PluginWrapper plugin = new PluginWrapper();

            if (plugin.loadState(file.toPath())) {
                addToList(plugin);
            } else {
                showError("Host Error", "State load is false");
            }
        }
    }

    protected void savePlugin() {
        PluginWrapper selected = pluginListView.getSelectionModel().getSelectedItem();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Steinberg VST State", "*." + selected.getStateExtension()),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        File file = fileChooser.showSaveDialog(stage);
        selected.saveState(file.toPath());
    }

    protected void showFileChooserAndAddPlugin() {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(stage);

        addPlugin(file);
    }

    public static final Map<String, Function<File, PluginWrapper>> loadPlugin = new HashMap<>(Map.ofEntries(
            Map.entry("vst3", file -> {
                if (file.getName().endsWith(PluginWrapper.Type.VST3.fileExtension[0])) {
                    IPluginWrapper<vst3> vst3PluginImpl = new VST3();

                    PluginWrapper plugin = new PluginWrapper(vst3PluginImpl);

                    try {
                        if (vst3PluginImpl.getPlugin().asyncInit(
                                file,
                                ConfigurationManager.instance.getIntItem("vst3_sample_rate", String.valueOf((int) MediaProcessor.mediaProcessor.mediaPlayer.getSampleRate())),
                                ConfigurationManager.instance.getIntItem("vst3_max_block_size", String.valueOf(MediaProcessor.mediaProcessor.MEDIA_PLAYER_BLOCK_SIZE_FRAMES)),
                                ConfigurationManager.instance.getIntItem("vst3_double_64_processing", "0"),
                                ConfigurationManager.instance.getBooleanItem("vst3_is_real_time_processing", "true")
                        ).get()) {
                            return plugin;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new VST3LoadException();
                    }
                }

                throw new VST3LoadException();
            }),
            Map.entry("dll", file -> {
                if (file.getName().endsWith(PluginWrapper.Type.VST.fileExtension[0])) {
                    try {
                        return new PluginWrapper(new VST(JVstHost24.newInstance(file,
                                ConfigurationManager.instance.getIntItem("vst_sample_rate", String.valueOf((int) MediaProcessor.mediaProcessor.mediaPlayer.getSampleRate())),
                                ConfigurationManager.instance.getIntItem("vst_block_size", String.valueOf(MediaProcessor.mediaProcessor.MEDIA_PLAYER_BLOCK_SIZE_FRAMES)))
                        ));
                    } catch (FileNotFoundException | JVstLoadException e) {
                        throw new RuntimeException(e);
                    }
                }

                throw new RuntimeException(new JVstLoadException(""));
            })
    ));

    protected String getExtension(String path) {
        if(!path.contains("."))
            return URI_NULL.getCode();

        return path.substring(path.lastIndexOf(".") + 1);
    }

    public void addPlugin(File file) {
        if (file == null) return;

        try {
            PluginWrapper plugin = loadPlugin
                    .get(getExtension(file.getAbsolutePath()))
                    .apply(file);

            if (plugin == null) {
                Music.mainLogger.severe("PluginWrapper is null after loadPlugin");
                return;
            }

            javafx.application.Platform.runLater(() -> {
                addToList(plugin);
            });
        } catch (Throwable ex) {
            Music.mainLogger.severe("loadPlugin failed: " + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void addToList(PluginWrapper plugin) {
        plugin.turnOn();
        AudioHost.instance.vstPlugins.add(plugin);
        pluginListView.getItems().add(plugin);
        rf.ebanina.UI.Editors.Player.Controller.updateMediaPlayerPlugins();
    }

    public void removeSelectedPlugin() {
        PluginWrapper selected = pluginListView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            //selected.destroy();
            pluginListView.getItems().remove(selected);
            AudioHost.instance.vstPlugins.remove(selected);
            rf.ebanina.UI.Editors.Player.Controller.updateMediaPlayerPlugins();
        }
    }

    public void removeAllPlugins() {
        List<PluginWrapper> pointer = MediaProcessor.mediaProcessor.mediaPlayer.getPlugins();

        MediaProcessor.mediaProcessor.mediaPlayer.getPlugins().clear();
        pluginListView.getItems().clear();
        AudioHost.instance.vstPlugins.clear();

        for (PluginWrapper pluginWrapper : pointer) {
            pluginWrapper.destroy();
        }
    }
}
