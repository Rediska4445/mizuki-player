package rf.ebanina.UI.UI.Paint.IdentifyColorAlgorithms;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import rf.ebanina.UI.UI.Paint.ColorPolicy;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

public class AttractiveColor implements ColorPolicy {
    /** * Преобразует RGB в HSL. * * @param r Красный канал. * @param g Зеленый канал. * @param b Синий канал. * @return Массив значений HSL. */
    private double[] rgbToHsl(double r, double g, double b) {
        r /= 255.0;
        g /= 255.0;
        b /= 255.0;

        double cmax = Math.max(Math.max(r, g), b);
        double cmin = Math.min(Math.min(r, g), b);
        double delta = cmax - cmin;

        double hue = calculateHue(r, g, b, cmax, delta);
        double saturation = calculateSaturation(cmax, delta);
        double lightness = (cmax + cmin) / 2.0;

        return new double[] {hue, saturation, lightness};
    }

    /** * Рассчитывает тон (Hue) в HSL. */
    private double calculateHue(double r, double g, double b, double cmax, double delta) {
        if (delta == 0) return 0;
        if (cmax == r) return ((g - b) / delta % 6);
        if (cmax == g) return ((b - r) / delta + 2);
        return ((r - g) / delta + 4);
    }

    /** * Рассчитывает насыщенность (Saturation) в HSL. */
    private double calculateSaturation(double cmax, double delta) {
        if (cmax == 0) return 0;
        return delta / cmax;
    }

    @Override
    public Color getColor(Image img) {
        PixelReader reader = img.getPixelReader();
        WritableImage writableImage = new WritableImage(reader, (int) img.getWidth(), (int) img.getHeight());

        double maxScore = 0;
        Color mostAttractingColor = Color.BLACK;

        for (int y = 0; y < writableImage.getHeight(); y++) {
            for (int x = 0; x < writableImage.getWidth(); x++) {
                Color pixelColor = reader.getColor(x, y);
                if (pixelColor.getBrightness() >= 0.95 || pixelColor.getBrightness() <= 0.05) continue;

                double hsl[] = rgbToHsl(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue());
                double saturation = hsl[1];
                double lightness = hsl[2];

                double score = saturation * lightness;

                if (score > maxScore) {
                    maxScore = score;
                    mostAttractingColor = pixelColor;
                }
            }
        }

        return mostAttractingColor;
    }

    @Override
    public String getName() {
        return ColorProcessor.IdentifyColorAlgorithms.ATTRACTING.getCode();
    }
}