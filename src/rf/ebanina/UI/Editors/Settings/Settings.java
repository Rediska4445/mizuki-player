package rf.ebanina.UI.Editors.Settings;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.AnimationDialog;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.io.IOException;

public final class Settings
        implements IEditor
{
    private Stage stageSet;

    private static Settings instance;

    private rf.ebanina.UI.Editors.Statistics.Track.Controller currentController;

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

            FXMLLoader loader = ResourceManager.Instance.loadFxmlLoader(
                    ResourceManager.Instance.resourcesPaths.get("FXMLTrackStatisticsPath")
            );
            Parent root = loader.load();

            this.currentController = loader.getController();

            AnimationDialog statsDialog = new AnimationDialog(stage, Root.rootImpl.getRoot());
            statsDialog.setDialogMaxSize(0.75, 0.85);
            statsDialog.setTopBorder(ColorProcessor.core.getMainClr());

            VBox dialogContent = statsDialog.getDialogBox();
            dialogContent.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);

            statsDialog.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
