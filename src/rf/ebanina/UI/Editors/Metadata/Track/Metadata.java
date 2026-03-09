package rf.ebanina.UI.Editors.Metadata.Track;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.UI.Editors.IEditor;

import java.awt.*;
import java.io.IOException;
import java.util.Map;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.UI.Root.windowsSizes;

public class Metadata implements IEditor {
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

    private Animation Blur(Node node, int dura, int newProperties) {
        GaussianBlur blur = (GaussianBlur) node.getEffect();
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(blur.radiusProperty(), blur.getRadius())),
                new KeyFrame(Duration.millis(dura),
                        new KeyValue(blur.radiusProperty(), newProperties))
        );

        return timeline;
    }

    @Override
    public void open(Stage stage) {
        try {
            Controller controller = new Controller();
            controller.setTrack((Track) track.clone());

            FXMLLoader loader = ResourceManager.Instance.loadFxmlLoader(
                    ResourceManager.Instance.resourcesPaths.get("FXMLMetaDataPath")
            );
            loader.setController(controller);

            Parent root = loader.load();

            Stage stageSet = new Stage();
            Scene scene = new Scene(root);

            stageSet.setTitle(track.viewName() + " - " + getLocaleString("window_title_metadata", "Metadata"));
            stageSet.initModality(Modality.WINDOW_MODAL);
            stageSet.initOwner(stage);
            stageSet.getIcons().add(ColorProcessor.logo);
            stageSet.setResizable(false);
            stageSet.setScene(scene);
            stageSet.show();

            Map.Entry<Point, Dimension> windowSize = windowsSizes.get(getClass().getName());
            if (windowSize != null) {
                stageSet.setWidth(windowSize.getValue().getWidth());
                stageSet.setHeight(windowSize.getValue().getHeight());
                stageSet.setX(windowSize.getKey().getX());
                stageSet.setY(windowSize.getKey().getY());
            }

            stageSet.setOnCloseRequest((e) -> {
                windowsSizes.put(getClass().getName(),
                        Map.entry(
                                new Point((int) stageSet.getX(), (int) stageSet.getY()),
                                new Dimension((int) scene.getWidth(), (int) scene.getHeight())
                        )
                );
                instance = null;
            });

            Root.rootImpl.setStageCaptionColor(stageSet, ColorProcessor.core.getMainClr());
            Blur(Root.root, 250, 20).play();

            stageSet.setOnCloseRequest((e) -> Blur(Root.root, 250, 0).play());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
