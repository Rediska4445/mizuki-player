package rf.ebanina.UI.UI.Context.Tooltip;

import javafx.animation.FadeTransition;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public class ContextTooltip extends Tooltip {
    public ContextTooltip(String text) {
        super(text);

        setOnShown(e -> {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), getSkin().getNode());
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });

        setOnHidden(e -> {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), getSkin().getNode());
            fadeIn.setFromValue(1);
            fadeIn.setToValue(0);
            fadeIn.play();
        });

    }
}
