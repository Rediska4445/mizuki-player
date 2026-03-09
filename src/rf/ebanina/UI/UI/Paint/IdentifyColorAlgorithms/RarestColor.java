package rf.ebanina.UI.UI.Paint.IdentifyColorAlgorithms;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import rf.ebanina.UI.UI.Paint.ColorPolicy;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.util.HashMap;
import java.util.Map;

public class RarestColor implements ColorPolicy {
    @Override
    public Color getColor(Image img) {
        PixelReader reader = img.getPixelReader();
        WritableImage writableImage = new WritableImage(reader, (int) img.getWidth(), (int) img.getHeight());

        Map<Color, Integer> colorFrequencies = new HashMap<>(); // Частоты встречаемости цветов

        // Проходим по каждому пикселю и считаем частоты цветов
        for (int y = 0; y < writableImage.getHeight(); y++) {
            for (int x = 0; x < writableImage.getWidth(); x++) {
                Color currentColor = reader.getColor(x, y);
                colorFrequencies.put(currentColor, colorFrequencies.getOrDefault(currentColor, 0) + 1);
            }
        }

        // Находим цвет с минимальной частотой
        Color rarestColor = null;
        int minFrequency = Integer.MAX_VALUE;

        for (Map.Entry<Color, Integer> entry : colorFrequencies.entrySet()) {
            if (entry.getValue() < minFrequency) {
                minFrequency = entry.getValue();
                rarestColor = entry.getKey();
            }
        }

        return rarestColor;
    }

    @Override
    public String getName() {
        return ColorProcessor.IdentifyColorAlgorithms.RAREST.getCode();
    }
}
