package rf.ebanina.UI.Editors.Statistics.Track;

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
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;

public class TrackStatistics implements IEditor {

    public static TrackStatistics instance = new TrackStatistics();

    @Override
    public void open(Stage stage) {
        open(stage, PlayProcessor.playProcessor.getTracks()
                .get(PlayProcessor.playProcessor.getTrackIter()));
    }

    public void open(Stage stage, Track track) {
        try {
            FXMLLoader loader = ResourceManager.Instance.loadFxmlLoader(
                    ResourceManager.Instance.resourcesPaths.get("FXMLTrackStatisticsPath")
            );
            Parent root = loader.load();

            Controller controller = loader.getController();
            controller.setTrack(track);

            AnimationDialog statsDialog = new AnimationDialog(stage, Root.rootImpl.getRoot());
            statsDialog.setDialogMaxSize(0.75, 0.85);
            statsDialog.setTopBorder(ColorProcessor.core.getMainClr());

            VBox dialogContent = statsDialog.getDialogBox();
            dialogContent.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);

            statsDialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
