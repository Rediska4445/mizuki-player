package rf.ebanina.UI.Editors.Settings;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.ebanina.Music;
import rf.ebanina.UI.Editors.IEditor;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.logo;
import static rf.ebanina.UI.Root.windowsSizes;

public final class Settings implements IEditor {
    private Stage stageSet;

    private static Settings instance;

    public static Settings getInstance() {
        if(instance == null) {
            return instance = new Settings();
        }

        return instance;
    }

    @Override
    public void open(Stage stage) {
        try {
            stageSet = new Stage();

            Parent root = ResourceManager.Instance.loadFXML("FXMLSettingsPath");
            Scene scene = new Scene(root);

            Map.Entry<Point, Dimension> windowSize = windowsSizes.get(getClass().getName());

            if(windowSize != null) {
                stageSet.setX(windowSize.getKey().getX());
                stageSet.setY(windowSize.getKey().getY());
                stageSet.setWidth(windowSize.getValue().getWidth());
                stageSet.setHeight(windowSize.getValue().getHeight());
            }

            stageSet.setTitle(Music.name + " - " + getLocaleString("gui_config", "Config"));
            stageSet.initModality(Modality.WINDOW_MODAL);
            stageSet.initOwner(stage);
            stageSet.getIcons().add(logo);
            stageSet.setResizable(true);
            stageSet.setOnCloseRequest((e) -> windowsSizes.put(getClass().getName(),
                    Map.entry(new Point((int) stageSet.getX(), (int) stageSet.getY()), new Dimension((int) scene.getWidth(), (int) scene.getHeight()))
            ));

            stageSet.setScene(scene);
            stageSet.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
