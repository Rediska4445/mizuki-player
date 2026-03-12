package rf.ebanina.UI.Editors.Network;

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

public class NetworkHost
        implements IEditor
{
    public static NetworkHost instance = new NetworkHost();

    @Override
    public void open(Stage stage) {
        try {
            Parent loader = ResourceManager.Instance.loadFXML("FXMLDownloadInetPath");

            AnimationDialog dialog = new AnimationDialog(stage, Root.rootImpl.getRoot());
            dialog.setDialogMaxSize(0.7, 0.85);
            dialog.setTopBorder(ColorProcessor.core.getMainClr());

            VBox dialogContent = dialog.getDialogBox();
            dialogContent.getChildren().add(loader);

            VBox.setVgrow(loader, Priority.ALWAYS);

            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
