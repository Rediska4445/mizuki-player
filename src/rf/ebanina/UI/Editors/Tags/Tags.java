package rf.ebanina.UI.Editors.Tags;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.ebanina.Player.Track;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.UI.Root.windowsSizes;

public final class Tags implements IEditor {
    private static Tags instance;

    public static Tags getInstance() {
        if(instance == null)
            return instance = new Tags();

        return instance;
    }

    private Track track;

    public void prepare(Track track) {
        this.track = track;
    }

    public void open(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(new File(
                    ResourceManager.Instance.getFullyPath(ResourceManager.Instance.resourcesPaths.get("FXMLTagEditorPath"))).toURI().toURL()
            );

            Parent root = loader.load();

            Controller controller = loader.getController();
            controller.setTrack(track);

            Stage tagsStage = new Stage();
            tagsStage.setTitle(getLocaleString("context_menu_edit_tags", "Edit tags") + ": " + track.viewName);
            tagsStage.setScene(new Scene(root));

            Map.Entry<Point, Dimension> windowSize = windowsSizes.get(getClass().getName());

            if(windowSize != null) {
                tagsStage.setWidth(windowSize.getValue().getWidth());
                tagsStage.setHeight(windowSize.getValue().getHeight());
                tagsStage.setX(windowSize.getKey().getX());
                tagsStage.setY(windowSize.getKey().getY());
            }

            tagsStage.setOnCloseRequest((e) -> {
                windowsSizes.put(getClass().getName(),
                        Map.entry(new Point((int) tagsStage.getX(), (int) tagsStage.getY()), new Dimension((int) tagsStage.getScene().getWidth(), (int) tagsStage.getScene().getHeight()))
                );

                instance = null;
            });

            tagsStage.initModality(Modality.APPLICATION_MODAL);
            tagsStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
