package rf.ebanina.UI.UI.Element.Slider;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Slider;

public class CircularSlider extends Slider {

    private final DoubleProperty radius = new SimpleDoubleProperty(35);

    public CircularSlider() {
        super();
        setMin(0);
        setMax(100);
        setValue(0);
        setSkin(new CircularSliderSkin(this));
    }

    public final double getRadius() {
        return radius.get();
    }

    public final void setRadius(double value) {
        radius.set(value);
    }

    public final DoubleProperty radiusProperty() {
        return radius;
    }
}
