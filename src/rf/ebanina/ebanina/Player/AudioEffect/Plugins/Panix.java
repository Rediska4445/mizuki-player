package rf.ebanina.ebanina.Player.AudioEffect.Plugins;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.AudioEffect.IAudioEffect;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Panix implements IAudioEffect {
    private AtomicReference<Float> currentPan = new AtomicReference<>(0.0f);

    private boolean isActive = true;

    @Override
    public String getName() {
        return "Panix";
    }

    @Override
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    private Slider panSlider;
    private Label panLabel;

    @Override
    public void openEditor(Stage parent) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        panLabel = new Label("Pan: " + String.format("%.2f", currentPan.get()) + "x");

        panSlider = new Slider(-1, 1, currentPan.get());
        panSlider.setShowTickLabels(true);
        panSlider.setShowTickMarks(true);
        panSlider.setMajorTickUnit(0.5);
        panSlider.setMinorTickCount(5);
        panSlider.setBlockIncrement(0.1);

        panSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentPan.set(newValue.floatValue());
            panLabel.setText("Pan: " + String.format("%.2f", currentPan.get()) + "x");
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> parent.close());

        root.getChildren().addAll(
                new Label("Pan"),
                panLabel,
                panSlider,
                closeButton
        );

        Scene scene = new Scene(root, 300, 150);
        parent.setTitle("Panix (Ebanina Std)");
        parent.setScene(scene);
        parent.setResizable(false);
        parent.show();
    }

    @Override
    public float[][] process(float[][] in, int frames) {
        if(isActive) {
            float[][] output = new float[in.length][frames];

            System.arraycopy(in, 0, output, 0, in.length);

            if (output.length >= 2) {
                float leftMul = (float) (Math.cos((currentPan.get() + 1) * Math.PI / 4));
                float rightMul = (float) (Math.cos((1 - currentPan.get()) * Math.PI / 4));

                for (int i = 0; i < output[0].length; i++) {
                    output[0][i] *= leftMul;
                    output[1][i] *= rightMul;
                }
            }

            return output;
        } else {
            return in;
        }
    }

    @Override
    public Map<String, String> load() {
        return Map.of(
                "plugin.panix.enable", String.valueOf(isActive),
                "plugin.panix.pan", String.valueOf(currentPan.get())
        );
    }
}
