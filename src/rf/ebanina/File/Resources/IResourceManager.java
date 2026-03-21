package rf.ebanina.File.Resources;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

import java.io.File;
import java.io.IOException;

public interface IResourceManager {
    void loadResources(String filePath);
    <R> R loadResource(Class<R> resourceClazz, String resourceType, String resourceId, String[] extensions);
    <R> R loadResource(String resourceId);

    String loadStylesheet(String name);
    String loadSVG(String id);
    Image loadImage(String image, int height, int width, boolean isPreserveRatio, boolean isSmooth);
    Font loadFont(String font, int size);
    Parent loadFXML(String path) throws IOException;
    FXMLLoader loadFxmlLoader(String path) throws IOException;
    File loadResourcePathFile(String name);
}
