package rf.ebanina.UI.UI.Element.Slider;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class CircularSliderSkin extends SkinBase<CircularSlider> {

    private final Pane pane;
    private final Circle track;
    private final Circle knob;
    private final Text valueText;

    protected CircularSliderSkin(CircularSlider control) {
        super(control);

        pane = new Pane();
        pane.setPrefSize(150, 150);

        track = new Circle(control.getRadius(), Color.LIGHTGRAY);
        track.setStroke(Color.GRAY);
        track.setStrokeWidth(5);

        knob = new Circle(10, Color.DODGERBLUE);
        knob.setStroke(Color.DARKBLUE);
        knob.setStrokeWidth(2);

        valueText = new Text();
        valueText.setFont(Font.font(24));
        valueText.setFill(Color.BLACK);

        pane.getChildren().addAll(track, knob, valueText);
        getChildren().add(pane);

        updateKnob();

        ChangeListener<Number> valueListener = (obs, oldVal, newVal) -> updateKnob();
        control.valueProperty().addListener(valueListener);

        // Новый слушатель для radius
        ChangeListener<Number> radiusListener = (obs, oldVal, newVal) -> {
            track.setRadius(newVal.doubleValue());
            updateKnob();
        };
        control.radiusProperty().addListener(radiusListener);

        // Установить радиус изначально
        track.setRadius(control.getRadius());

        pane.setOnMousePressed(this::mouseHandler);
        pane.setOnMouseDragged(this::mouseHandler);
    }

    private void updateKnob() {
        CircularSlider control = getSkinnable();
        double val = control.getValue();

        double radius = control.getRadius();
        double angle = (val / 100) * 360 - 90;
        double rad = Math.toRadians(angle);

        double centerX = pane.getWidth() / 2;
        double centerY = pane.getHeight() / 2;

        track.setCenterX(centerX);
        track.setCenterY(centerY);

        double knobX = centerX + radius * Math.cos(rad);
        double knobY = centerY + radius * Math.sin(rad);

        knob.setCenterX(knobX);
        knob.setCenterY(knobY);

        valueText.setText(String.format("%.0f", val));
        valueText.setLayoutX(centerX - valueText.getLayoutBounds().getWidth() / 2);
        valueText.setLayoutY(centerY + valueText.getLayoutBounds().getHeight() / 4);
    }

    private void mouseHandler(MouseEvent e) {
        CircularSlider control = getSkinnable();
        double centerX = pane.getWidth() / 2;
        double centerY = pane.getHeight() / 2;

        double dx = e.getX() - centerX;
        double dy = e.getY() - centerY;

        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (angle < 0) angle += 360;

        control.setValue((angle / 360) * 100);
    }

    @Override
    protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
        pane.resizeRelocate(contentX, contentY, contentWidth, contentHeight);
        updateKnob();
    }
}