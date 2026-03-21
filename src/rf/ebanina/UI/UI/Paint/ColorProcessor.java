package rf.ebanina.UI.UI.Paint;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Paint.IdentifyColorAlgorithms.*;

import java.util.ArrayList;
import java.util.List;

import static rf.ebanina.utils.Math.scale;

public class ColorProcessor {
    public static final boolean album_art_parsed_set_in_tags = ConfigurationManager.instance.getBooleanItem("album_art_parsed_set_in_tags", "false");

    protected String color_type = ConfigurationManager.instance.getItem("album_art_get_color_type", "average");

    protected SimpleIntegerProperty hueProperty = new SimpleIntegerProperty(0);

    public int getHue() {
        return hueProperty.get();
    }

    public SimpleIntegerProperty huePropertyProperty() {
        return hueProperty;
    }

    public void setHue(int hueProperty) {
        this.hueProperty.set(hueProperty);
    }

    public static final int size = ConfigurationManager.instance.getIntItem("album_art_image_size", "200");
    public static final boolean isSmooth = true;
    public static final boolean isPreserveRatio = false;

    public static final javafx.scene.image.Image logo = ResourceManager.Instance.loadImage("album_art_logo", size, size, isPreserveRatio, isSmooth);
    public static final ImagePattern logoPattern = new ImagePattern(logo);

    public static ColorProcessor core = new ColorProcessor();

    protected ObjectProperty<Paint> mainClr = new SimpleObjectProperty<>();

    public ObjectProperty<Paint> mainClrProperty() {
        return mainClr;
    }

    public Color getMainClr() {
        return (Color) mainClr.get();
    }

    public void setMainClr(Paint mainClr) {
        this.mainClr.set(mainClr);
    }

    public String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }

    public Color setTransparent(Color color, double opacity) {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }

    public void scaleHue(double val) {
        if(val > 1) {
            core.setHue((int) val / 360);
        } else if(val < 1) {
            core.setHue((int) -(val / 360));
        } else {
            core.setHue(0);
        }

        core.setHue((int) scale(val, 1, 1.5, 0, 360));
    }

    public javafx.scene.image.Image changeHue(javafx.scene.image.Image source, double hueDegrees) {
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        WritableImage result = new WritableImage(width, height);

        PixelReader reader = source.getPixelReader();
        PixelWriter writer = result.getPixelWriter();

        double hueShift = ((hueDegrees % 360) + 360) % 360;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);

                float[] hsb = rgbToHsb(color.getRed(), color.getGreen(), color.getBlue());

                double newHue = (hsb[0] * 360 + hueShift) % 360;
                float newHueNorm = (float)(newHue / 360);

                Color newColor = Color.hsb(
                        newHueNorm * 360,
                        hsb[1],
                        hsb[2],
                        color.getOpacity());

                writer.setColor(x, y, newColor);
            }
        }

        return result;
    }

    private float[] rgbToHsb(double r, double g, double b) {
        float[] hsb = new float[3];

        java.awt.Color.RGBtoHSB(
                (int)(r * 255),
                (int)(g * 255),
                (int)(b * 255),
                hsb
        );

        return hsb;
    }

    public final List<ColorPolicy> identifyColorAlgorithms = new ArrayList<>(List.of(
            new AverageColor(),
            new ContrastColor(),
            new AttractiveColor(),
            new RarestColor(),
            new UniqueColor()
    ));

    public Color getGeneralColorFromImage(javafx.scene.image.Image image) {
        if(image == null) {
            return Color.BLACK;
        }

        for(ColorPolicy colorPolicy : identifyColorAlgorithms) {
            if(color_type.equalsIgnoreCase(colorPolicy.getName())) {
                return colorPolicy.getColor(image);
            }
        }

        return Color.BLACK;
    }

    public enum IdentifyColorAlgorithms {
        CONTRAST("contrast"),
        AVERAGE("average"),
        ATTRACTING("attracting"),
        UNIQUE("unique"),
        RAREST("rarest");

        private String code;

        public String getCode() {
            return code;
        }

        IdentifyColorAlgorithms(String code) {
            this.code = code;
        }
    }
}