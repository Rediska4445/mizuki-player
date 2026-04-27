package rf.ebanina.UI.Editors.Settings;

import javafx.scene.Parent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.UI.Editors.IViewable;
import rf.ebanina.UI.Editors.Viewable;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.Dialogs.AnimationDialog;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.io.IOException;

@Viewable
public final class Settings
        implements IEditor, IViewable
{
    private static Settings instance;

    public static Settings getInstance() {
        if(instance == null) {
            return instance = new Settings();
        }

        return instance;
    }

    @Override
    public void open(Stage stage) {
        try {
            Parent root = parent();

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

    @Override
    public Parent parent() throws IOException {
        return ResourceManager.Instance.loadFXML("FXMLSettingsPath");
    }

    @Override
    public String name() {
        return ResourceManager.getLocaleString("viewable_item_name_settings", "Settings");
    }

    @Override
    public String description() {
        return ResourceManager.getLocaleString("viewable_item_description_settings", "Description");
    }
}
