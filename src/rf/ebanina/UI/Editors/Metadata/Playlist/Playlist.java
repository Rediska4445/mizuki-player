package rf.ebanina.UI.Editors.Metadata.Playlist;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.UI.Root.Blur;
import static rf.ebanina.UI.Root.windowsSizes;

public class Playlist implements IEditor {
    private rf.ebanina.ebanina.Player.Playlist playlist;

    public void prepare(rf.ebanina.ebanina.Player.Playlist playlist) {
        this.playlist = playlist;
    }

    @Override
    public void open(Stage stage) {
        try {
            rf.ebanina.UI.Editors.Metadata.Playlist.Controller controller = new rf.ebanina.UI.Editors.Metadata.Playlist.Controller();
            controller.setPlaylist((rf.ebanina.ebanina.Player.Playlist) playlist.clone());

            FXMLLoader loader = ResourceManager.Instance.loadFxmlLoader(
                    ResourceManager.Instance.resourcesPaths.get("FXMLMetaDataPath")
            );
            loader.setController(controller);

            Parent root = loader.load();

            Stage stageSet = new Stage();
            Scene scene = new Scene(root);

            stageSet.setTitle(playlist.getName() + " - " + getLocaleString("window_title_metadata", "Metadata"));
            stageSet.initModality(Modality.WINDOW_MODAL);
            stageSet.initOwner(stage);
            stageSet.getIcons().add(ColorProcessor.logo);
            stageSet.setResizable(false);
            stageSet.setScene(scene);
            stageSet.show();

            Map.Entry<Point, Dimension> windowSize = windowsSizes.get(getClass().getName());
            if (windowSize != null) {
                stageSet.setWidth(windowSize.getValue().getWidth());
                stageSet.setHeight(windowSize.getValue().getHeight());
                stageSet.setX(windowSize.getKey().getX());
                stageSet.setY(windowSize.getKey().getY());
            }

            stageSet.setOnCloseRequest((e) -> {
                windowsSizes.put(getClass().getName(),
                        Map.entry(
                                new Point((int) stageSet.getX(), (int) stageSet.getY()),
                                new Dimension((int) scene.getWidth(), (int) scene.getHeight())
                        )
                );
            });

            Blur(Root.root, 250, 20).play();

            stageSet.setOnCloseRequest((e) -> Blur(Root.root, 250, 0).play());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
