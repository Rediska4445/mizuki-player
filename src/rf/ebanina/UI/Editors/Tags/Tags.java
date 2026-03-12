package rf.ebanina.UI.Editors.Tags;

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

import java.io.File;
import java.io.IOException;

public final class Tags
        implements IEditor
{
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

    public void open(Stage ownerStage) {
        try {
            FXMLLoader loader = new FXMLLoader(new File(
                    ResourceManager.Instance.getFullyPath(ResourceManager.Instance.resourcesPaths.get("FXMLTagEditorPath"))
            ).toURI().toURL());

            Parent root = loader.load();

            Controller controller = loader.getController();
            controller.setTrack(track);

            AnimationDialog tagsDialog = new AnimationDialog(ownerStage, Root.rootImpl.getRoot());

            VBox dialogContent = tagsDialog.getDialogBox();
            dialogContent.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
            tagsDialog.setDialogMaxSize(0.2, 0.6);
            tagsDialog.animationTopBorder(ColorProcessor.core.getMainClr()).play();
            tagsDialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
