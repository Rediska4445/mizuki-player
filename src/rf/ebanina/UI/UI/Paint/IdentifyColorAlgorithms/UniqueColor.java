package rf.ebanina.UI.UI.Paint.IdentifyColorAlgorithms;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import rf.ebanina.UI.UI.Paint.ColorPolicy;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UniqueColor implements ColorPolicy {
    @Override
    public Color getColor(Image img) {
        return getUniqueColorsFromImage(img, 4, 125).get(0);
    }

    private Map<Color, Integer> getCountUniqueElements(int[] arrayList) {
        Map<Color, Integer> counter = new HashMap<>();

        for (int x : arrayList) {
            Color color = Color.rgb(x >> 16 & 0xff, x >> 8 & 0xff, x & 0xff);
            int newValue = counter.getOrDefault(color, 0) + 1;
            counter.put(color, newValue);
        }

        return counter;
    }

    public ArrayList<Color> getUniqueColorsFromImage(Image in, int mesh_width, int pixel) {
        BufferedImage image = SwingFXUtils.fromFXImage(in, null);

        int[] rgb = new int[(image.getHeight() / mesh_width) * (image.getWidth() / mesh_width)];

        for (int x = 0; x < image.getHeight() / mesh_width; x++) {
            for (int y = 0; y < image.getWidth() / mesh_width; y++) {
                int buff_x = x * mesh_width;
                int buff_y = y * mesh_width;

                rgb[x * y] = image.getRGB(buff_x, buff_y);
            }
        }

        Map<Color, Integer> a = getCountUniqueElements(rgb);

        int blackly = 0;
        int whitely = 0;

        ArrayList<Color> black = new ArrayList<>();
        ArrayList<Color> white = new ArrayList<>();

        for (Map.Entry<Color, Integer> entry : a.entrySet()) {
            if(((int) (entry.getKey().getRed() * 255) >= pixel)
                    || (((int) (entry.getKey().getGreen() * 255) >= pixel))
                    || (((int) (entry.getKey().getBlue() * 255)) >= pixel)) {
                whitely++;
                white.add(entry.getKey());
            } else {
                blackly++;
                black.add(entry.getKey());
            }
        }

        return whitely > blackly ? black : white;
    }

    @Override
    public String getName() {
        return ColorProcessor.IdentifyColorAlgorithms.UNIQUE.getCode();
    }
}
