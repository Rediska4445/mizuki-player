package rf.ebanina.UI.Editors.Info.Application;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import rf.ebanina.UI.Root;

import java.net.URL;
import java.util.ResourceBundle;

public class ApplicationInfoController
        implements Initializable
{
    @FXML
    private AnchorPane root;
    @FXML private ScrollPane scrollView;
    @FXML private VBox infoBox;
    @FXML private StackPane imageBox;
    @FXML private ImageView logo;

    private final Interpolator smoother = Root.iceInterpolator;

    private double scrollTarget = 0.0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logo.fitWidthProperty().bind(infoBox.widthProperty().multiply(0.7));
        logo.fitHeightProperty().bind(logo.fitWidthProperty()
                .multiply(logo.getImage().getHeight() / logo.getImage().getWidth()));

        scrollView.addEventFilter(ScrollEvent.SCROLL, ev -> {
            double deltaY = ev.getDeltaY();
            double contentHeight = infoBox.getBoundsInLocal().getHeight();
            double scrollStep = deltaY / contentHeight * 4;

            scrollTarget = Math.max(0, Math.min(1, scrollTarget - scrollStep));

            Timeline timeline = new Timeline(
                    new KeyFrame(
                            Duration.millis(700),
                            new KeyValue(scrollView.vvalueProperty(), scrollTarget, smoother)
                    )
            );
            timeline.play();

            ev.consume();
        });
    }
}
