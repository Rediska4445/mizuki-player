package rf.ebanina.UI.Editors.Player;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.AnimationDialog;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioHost
        implements IEditor
{
    public static final AudioHost instance = new AudioHost();

    public List<PluginWrapper> vstPlugins = new ArrayList<>();

    @Override
    public void open(Stage ownerStage) {
        try {
            Parent parent = ResourceManager.Instance.loadFXML("FXMLAudioHostPath");

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
}
