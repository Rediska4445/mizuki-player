package rf.ebanina.UI.Editors.Info.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IViewable;
import rf.ebanina.UI.Editors.Metadata.Track.Controller;

import java.io.IOException;

public class ApplicationInfo
        implements IViewable
{
    protected Parent parent;

    public ApplicationInfo() {}

    @Override
    public Parent parent() throws IOException {
        FXMLLoader loader = ResourceManager.getInstance().loadFxmlLoader(
                ResourceManager.getInstance().getResourcesPaths().get("FXMLApplicationInfo")
        );

        return parent = loader.load();
    }

    @Override
    public String name() {
        return ResourceManager.getInstance().getLocalizationManager().getLocalizationString("viewable_item_name_main", "Main");
    }

    @Override
    public String description() {
        return ResourceManager.getInstance().getLocalizationManager().getLocalizationString("viewable_item_description_main", "Main");
    }
}