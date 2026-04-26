package rf.ebanina.UI.Editors.Player;

import com.synthbot.audioplugin.vst.JVstLoadException;
import com.synthbot.audioplugin.vst.vst2.JVstHost24;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.UI.Editors.IViewable;
import rf.ebanina.UI.Editors.Viewable;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.Dialogs.AnimationDialog;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.AudioPlugins.IPluginWrapper;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST3;
import rf.ebanina.ebanina.Player.AudioPlugins.VST.VST3LoadException;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.vst3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@Viewable
public class AudioHost
        implements IEditor, IViewable
{
    public static AudioHost instance = new AudioHost();

    public List<PluginWrapper> vstPlugins = new ArrayList<>();

    public static AudioHost defaultInstance() {
        return new AudioHost();
    }

    public static AudioHost getInstance() {
        if(instance == null)
            instance = defaultInstance();

        return instance;
    }

    @Override
    public void open(Stage ownerStage) {
        try {
            Parent parent = parent();

            AnimationDialog hostDialog = new AnimationDialog(ownerStage, Root.rootImpl.getRoot());
            hostDialog.setDialogMaxSize(0.75, 0.75);
            hostDialog.setTopBorder(ColorProcessor.core.getMainClr());

            VBox dialogContent = hostDialog.getDialogBox();
            dialogContent.getChildren().add(parent);

            VBox.setVgrow(parent, Priority.ALWAYS);

            Scene scene = parent.getScene();
            if (scene != null) {
                scene.getStylesheets().add(ResourceManager.Instance.loadStylesheet("vsthost"));
            }

            javafx.application.Platform.runLater(() -> {
                hostDialog.show();
                hostDialog.animationTopBorder(ColorProcessor.core.getMainClr()).play();
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Parent parent() throws IOException {
        return ResourceManager.Instance.loadFXML("FXMLAudioHostPath");
    }

    @Override
    public String name() {
        return LocalizationManager.getLocaleString("viewable_item_name_audio_host", "Audio-Host");
    }

    @Override
    public String description() {
        return LocalizationManager.getLocaleString("viewable_item_description_audio_host", "Description");
    }

    protected final Map<String, Function<File, PluginWrapper>> loadPlugin = new HashMap<>(Map.ofEntries(
            Map.entry(PluginWrapper.Type.VST3.fileExtension[0], file -> {
                IPluginWrapper<vst3> vst3PluginImpl = new VST3();
                PluginWrapper plugin = new PluginWrapper(vst3PluginImpl);

                try {
                    if (vst3PluginImpl.getPlugin().asyncInit(
                            file,
                            ConfigurationManager.instance.getIntItem("vst3_sample_rate",
                                    String.valueOf((int) MediaProcessor.mediaProcessor.mediaPlayer.getSampleRate())),
                            ConfigurationManager.instance.getIntItem("vst3_max_block_size",
                                    String.valueOf(MediaProcessor.mediaProcessor.MEDIA_PLAYER_BLOCK_SIZE_FRAMES)),
                            ConfigurationManager.instance.getIntItem("vst3_double_64_processing", "0"),
                            ConfigurationManager.instance.getBooleanItem("vst3_is_real_time_processing", "true")
                    ).get()) {
                        return plugin;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new VST3LoadException();
                }

                throw new VST3LoadException();
            }),
            Map.entry(PluginWrapper.Type.VST.fileExtension[0], file -> {
                try {
                    return new PluginWrapper(new VST(JVstHost24.newInstance(file,
                            ConfigurationManager.instance.getIntItem("vst_sample_rate", String.valueOf((int) MediaProcessor.mediaProcessor.mediaPlayer.getSampleRate())),
                            ConfigurationManager.instance.getIntItem("vst_block_size", String.valueOf(MediaProcessor.mediaProcessor.MEDIA_PLAYER_BLOCK_SIZE_FRAMES)))
                    ));
                } catch (FileNotFoundException | JVstLoadException e) {
                    throw new RuntimeException(e);
                }
            })
    ));

    public Map<String, Function<File, PluginWrapper>> getLoadPlugin() {
        return loadPlugin;
    }
}