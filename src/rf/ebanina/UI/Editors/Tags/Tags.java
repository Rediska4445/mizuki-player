package rf.ebanina.UI.Editors.Tags;

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
import rf.ebanina.UI.UI.Element.Dialogs.AnimationDialog;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.io.File;
import java.io.IOException;

@Viewable
public final class Tags
        implements IEditor, IViewable
{
    private static Tags instance;

    public static Tags getInstance() {
        if(instance == null)
            return instance = new Tags();

        return instance;
    }

    private Track track;

    public void setTrack(Track track) {
        this.track = track;
    }

    private rf.ebanina.UI.Editors.Statistics.Track.Controller currentController;

    public rf.ebanina.UI.Editors.Statistics.Track.Controller currentController() {
        return currentController;
    }

    @Override
    public void open(Stage ownerStage) {
        try {
            Parent root = parent();

            AnimationDialog tagsDialog = new AnimationDialog(ownerStage, Root.rootImpl.getRoot());

            VBox dialogContent = tagsDialog.getDialogBox();
            dialogContent.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
            tagsDialog.setDialogMaxSize(0.2, 0.6);
            tagsDialog.animationTopBorder(ColorProcessor.core.getGeneralColorFromImage(track.getAlbumArt())).play();
            tagsDialog.show();

            tagsDialog.setOnHide((e) -> track = null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Parent parent() throws IOException {
        track = track == null ? PlayProcessor.playProcessor.getCurrentTrack() : track;

        Music.mainLogger.info(track);

        FXMLLoader loader = new FXMLLoader(new File(
                ResourceManager.Instance.getFullyPath(ResourceManager.Instance.resourcesPaths.get("FXMLTagEditorPath"))
        ).toURI().toURL());

        Parent root = loader.load();
        Controller controller = loader.getController();
        controller.setTrack(track);

        return root;
    }

    @Override
    public String name() {
        return LocalizationManager.getLocaleString("viewable_item_name_tags", "Tags");
    }

    @Override
    public String description() {
        return LocalizationManager.getLocaleString("viewable_item_description_tags", "Description");
    }
}
