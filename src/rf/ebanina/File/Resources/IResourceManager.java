package rf.ebanina.File.Resources;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

import java.io.File;
import java.io.IOException;

public interface IResourceManager
        extends IResource
{
    void loadResources(String filePath);

    String loadStylesheet(String name);
    String loadSVG(String id);
    Image loadImage(String image, int height, int width, boolean isPreserveRatio, boolean isSmooth);
    Font loadFont(String font, int size);
    Parent loadFXML(String path) throws IOException;
    FXMLLoader loadFxmlLoader(String path) throws IOException;
    File loadResourcePathFile(String name);
}
