package rf.ebanina.UI.Editors.Network;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Editors.IEditor;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.UI.Root.windowsSizes;
import static rf.ebanina.ebanina.Music.name;

public class NetworkHost implements IEditor {
    public static NetworkHost instance = new NetworkHost();

    @Override
    public void open(Stage stage) {
        try {
            Parent loader = ResourceManager.Instance.loadFXML("FXMLDownloadInetPath");
            Scene scene = new Scene(loader);

            Stage itStage = new Stage();
            itStage.setTitle(name + " - " + getLocaleString("window_title_download_inet", "Network Host"));
            itStage.getIcons().add(ColorProcessor.logo);
            itStage.setScene(scene);

            Map.Entry<Point, Dimension> windowSize = windowsSizes.get(Controller.class.getName());

            if(windowSize != null) {
                itStage.setWidth(windowSize.getValue().getWidth());
                itStage.setHeight(windowSize.getValue().getHeight());
                itStage.setX(windowSize.getKey().getX());
                itStage.setY(windowSize.getKey().getY());
            }

            itStage.setOnCloseRequest((e) -> windowsSizes.put(Controller.class.getName(), Map.entry(
                    new Point((int) itStage.getX(), (int) itStage.getY()), new Dimension((int) scene.getWidth(), (int) scene.getHeight())
            )));

            itStage.setResizable(true);
            itStage.show();

            Root.rootImpl.setStageCaptionColor(itStage, ColorProcessor.core.getMainClr());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
