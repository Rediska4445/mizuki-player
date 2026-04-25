package rf.ebanina.UI.Editors.Metadata.Track;

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
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;

@Viewable
public class Metadata
        implements IEditor, IViewable
{
    protected static Metadata instance;
    protected Parent parent;
    protected Track track;

    public Metadata() {}

    public static synchronized Metadata getInstance() {
        if (instance == null) {
            instance = new Metadata();
        }

        return instance;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    @Override
    public void open(Stage ownerStage) {
        try {
            Parent root = parent();

            AnimationDialog metaDialog = new AnimationDialog(ownerStage, Root.rootImpl.getRoot());
            metaDialog.setDialogMaxSize(0.35, 0.75);
            metaDialog.setTopBorder(ColorProcessor.core.getGeneralColorFromImage(track.getAlbumArt()));

            VBox dialogContent = metaDialog.getDialogBox();
            dialogContent.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);

            metaDialog.show();

            metaDialog.setOnHide((e) -> track = null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Parent parent() throws IOException {
        FXMLLoader loader = ResourceManager.Instance.loadFxmlLoader(
                ResourceManager.Instance.resourcesPaths.get("FXMLMetaDataPath")
        );

        Controller controller = new Controller();
        controller.setTrack(track == null ? PlayProcessor.playProcessor.getCurrentTrack() : track);

        loader.setController(controller);

        return parent = loader.load();
    }

    @Override
    public String name() {
        return LocalizationManager.getLocaleString("metadata_host_title", "Metadata");
    }

    @Override
    public String description() {
        return LocalizationManager.getLocaleString("metadata_host_description", "Description");
    }
}
