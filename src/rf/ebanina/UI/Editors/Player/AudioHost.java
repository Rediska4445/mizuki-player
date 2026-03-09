package rf.ebanina.UI.Editors.Player;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.UI.Editors.IEditor;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.UI.Root.windowsSizes;

public class AudioHost implements IEditor {
    public static final AudioHost instance = new AudioHost();

    public List<PluginWrapper> vstPlugins = new ArrayList<>();

    @Override
    public void open(Stage stage) {
        Scene scene;

        Stage hostStage = new Stage();

        Map.Entry<Point, Dimension> windowSize = windowsSizes.get(getClass().getName());

        if(windowSize != null) {
            hostStage.setX(windowSize.getKey().getX());
            hostStage.setY(windowSize.getKey().getY());
            hostStage.setWidth(windowSize.getValue().getWidth());
            hostStage.setHeight(windowSize.getValue().getHeight());
        }

        try {
            Parent parent = ResourceManager.Instance.loadFXML("FXMLAudioHostPath");

            scene = new Scene(parent);
            scene.getStylesheets().add(ResourceManager.Instance.loadStylesheet("vsthost"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        hostStage.setOnCloseRequest((e) -> windowsSizes.put(getClass().getName(),
                Map.entry(new Point((int) hostStage.getX(), (int) hostStage.getY()), new Dimension((int) scene.getWindow().getWidth(), (int) scene.getWindow().getHeight()))
        ));

        hostStage.initOwner(stage);
        hostStage.setTitle(Music.name + " - " + getLocaleString("window_title_host", "Audio-Host"));
        hostStage.setScene(scene);
        hostStage.show();

        Root.setStageCaptionColor(hostStage, ColorProcessor.core.getMainClr());
    }
}
