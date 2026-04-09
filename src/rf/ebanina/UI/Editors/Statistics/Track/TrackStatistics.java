package rf.ebanina.UI.Editors.Statistics.Track;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.UI.Editors.IViewable;
import rf.ebanina.UI.Editors.Viewable;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.AnimationDialog;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.loggining.Prefix;

import java.io.IOException;

@Viewable
public class TrackStatistics
        implements IEditor, IViewable
{
    public static TrackStatistics instance = new TrackStatistics();

    protected Controller currentController;

    protected Track track;

    public Controller currentController() {
        return currentController;
    }

    @Override
    public void open(Stage stage) {
        open(stage, PlayProcessor.playProcessor.getTracks()
                .get(PlayProcessor.playProcessor.getTrackIter()));
    }

    public TrackStatistics setCurrentController(Controller currentController) {
        this.currentController = currentController;
        return this;
    }

    public Controller getCurrentController() {
        return currentController();
    }

    public void open(Stage stage, Track track) {
        try {
            Music.mainLogger.println(Prefix.SUPPRESS, "this.track = " + this.track, stackTraceElement -> true);

            this.track = track;

            Music.mainLogger.println(Prefix.SUPPRESS, "this.track = " + this.track, stackTraceElement -> true);

            Parent root = parent();

            Music.mainLogger.println(Prefix.SUPPRESS, "this.track = " + this.track, stackTraceElement -> true);

            AnimationDialog statsDialog = new AnimationDialog(stage, Root.rootImpl.getRoot());
            statsDialog.setDialogMaxSize(0.75, 0.85);
            statsDialog.setTopBorder(ColorProcessor.core.getGeneralColorFromImage(track.getAlbumArt()));

            VBox dialogContent = statsDialog.getDialogBox();
            dialogContent.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);

            statsDialog.show();

            Music.mainLogger.println(Prefix.SUPPRESS, "this.track = " + this.track, stackTraceElement -> true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Parent parent() throws IOException {
        Music.mainLogger.println(Prefix.SUPPRESS, "this.track = " + this.track, stackTraceElement -> true);

        FXMLLoader loader = ResourceManager.Instance.loadFxmlLoader(
                ResourceManager.Instance.resourcesPaths.get("FXMLTrackStatisticsPath")
        );

        Music.mainLogger.println(Prefix.SUPPRESS, "this.track = " + this.track, stackTraceElement -> true);

        Parent root = loader.load();

        Music.mainLogger.println(Prefix.SUPPRESS, "this.track = " + this.track, stackTraceElement -> true);

        Controller controller = loader.getController();
        controller.setTrack(track);
        controller.initializeData();

        Music.mainLogger.println(Prefix.SUPPRESS, "this.track = " + this.track, stackTraceElement -> true);

        this.currentController = controller;

        return root;
    }

    @Override
    public String name() {
        return LocalizationManager.getLocaleString("window_statistics_title", "Tracks Statistics");
    }

    @Override
    public String description() {
        return LocalizationManager.getLocaleString("track_statistics_description", "Description");
    }
}
