package rf.ebanina.UI.Editors.Metadata.Track;

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
import rf.ebanina.ebanina.Player.Track;

import java.io.IOException;

public class Metadata
        implements IEditor
{
    private static Metadata instance;

    private Metadata() {}

    public static synchronized Metadata getInstance() {
        if (instance == null) {
            instance = new Metadata();
        }
        return instance;
    }

    private Track track;

    public void prepare(Track track) {
        this.track = track;
    }

    @Override
    public void open(Stage ownerStage) {
        try {
            Controller controller = new Controller();
            controller.setTrack((Track) track.clone());

            FXMLLoader loader = ResourceManager.Instance.loadFxmlLoader(
                    ResourceManager.Instance.resourcesPaths.get("FXMLMetaDataPath")
            );
            loader.setController(controller);
            Parent root = loader.load();

            AnimationDialog metaDialog = new AnimationDialog(ownerStage, Root.rootImpl.getRoot());
            metaDialog.setDialogMaxSize(0.35, 0.75);
            metaDialog.setTopBorder(ColorProcessor.core.getMainClr());

            VBox dialogContent = metaDialog.getDialogBox();
            dialogContent.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);

            metaDialog.show();
        } catch (IOException | CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }
}
