package rf.ebanina.UI.UI.Paint.IdentifyColorAlgorithms;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import rf.ebanina.UI.UI.Paint.ColorPolicy;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.awt.image.BufferedImage;

public class AverageColor implements ColorPolicy {
    private Color getAverageColor(BufferedImage image0) {
        try {
            int RED = 1;
            int GREEN = 1;
            int BLUE = 1;

            int square = image0.getHeight() * image0.getWidth();

            for (int x = 0; x < image0.getHeight(); x++) {
                for (int y = 0; y < image0.getWidth(); y++) {
                    RED += image0.getRGB(x, y) >> 16 & 0xff;
                    GREEN += image0.getRGB(x, y) >> 8 & 0xff;
                    BLUE += image0.getRGB(x, y) & 0xff;
                }
            }

            if ((RED / square) >= 175 && (GREEN / square) >= 175 && (BLUE / square) >= 175) {
                return Color.BLACK;
            }

            return Color.rgb(RED / square, GREEN / square, BLUE / square);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }

    @Override
    public Color getColor(Image img) {
        return getAverageColor(SwingFXUtils.fromFXImage(img, null));
    }

    @Override
    public String getName() {
        return ColorProcessor.IdentifyColorAlgorithms.AVERAGE.getCode();
    }
}