package rf.ebanina.UI.UI.Element.Text.Skins;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.scene.control.Label;
import javafx.scene.control.skin.LabelSkin;
import javafx.scene.text.Text;
import javafx.util.Duration;
import rf.ebanina.UI.Root;

public class FadeTextSkin
        extends LabelSkin
{
    public FadeTextSkin(Label control) {
        super(control);

        control.textProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && !oldVal.isEmpty() && !oldVal.equals(newVal)) {
                animate(oldVal);
            }
        });
    }

    private void animate(String oldTextValue) {
        Text currentTextNode = (Text) getSkinnable().lookup(".text");
        if (currentTextNode == null) return;

        Text oldTextNode = new Text(oldTextValue);
        oldTextNode.setFont(getSkinnable().getFont());
        oldTextNode.setFill(getSkinnable().getTextFill());

        oldTextNode.setLayoutX(currentTextNode.getLayoutX());
        oldTextNode.setLayoutY(currentTextNode.getLayoutY());

        getChildren().add(oldTextNode);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), oldTextNode);
        fadeOut.setInterpolator(Root.iceInterpolator);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> getChildren().remove(oldTextNode));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), currentTextNode);
        fadeIn.setInterpolator(Root.iceInterpolator);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        fadeOut.play();
        fadeIn.play();
    }
}
