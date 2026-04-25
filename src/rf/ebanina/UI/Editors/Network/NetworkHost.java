package rf.ebanina.UI.Editors.Network;

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

import java.io.IOException;

@Viewable
public class NetworkHost
        implements IEditor, IViewable
{
    public static NetworkHost instance = new NetworkHost();

    protected Parent parent;

    public NetworkHost() {}

    @Override
    public void open(Stage stage) {
        try {
            Parent loader = parent();

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

    @Override
    public Parent parent()
            throws IOException
    {
        if(parent == null)
            return parent = ResourceManager.Instance.loadFXML("FXMLDownloadInetPath");

        return parent;
    }

    @Override
    public String name() {
        return LocalizationManager.getLocaleString("network_host_title", "Network-Host");
    }

    @Override
    public String description() {
        return LocalizationManager.getLocaleString("network_host_description", "Description");
    }
}
