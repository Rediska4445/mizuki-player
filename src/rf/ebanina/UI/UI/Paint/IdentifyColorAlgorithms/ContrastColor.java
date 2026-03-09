package rf.ebanina.UI.UI.Paint.IdentifyColorAlgorithms;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import rf.ebanina.UI.UI.Paint.ColorPolicy;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.awt.image.BufferedImage;

public final class ContrastColor implements ColorPolicy {
    @Override
    public Color getColor(Image img) {
        return findMostContrastingColor(SwingFXUtils.fromFXImage(img, null));
    }

    @Override
    public String getName() {
        return ColorProcessor.IdentifyColorAlgorithms.CONTRAST.getCode();
    }

    private static class ColorLab {
        private final double L;
        private final double a;
        private final double b;
        private final int r;
        private final int g;
        private final int bl;

        public ColorLab(double L, double a, double b, int r, int g, int bl) {
            this.L = L;
            this.a = a;
            this.b = b;
            this.r = r;
            this.g = g;
            this.bl = bl;
        }

        public double getL() { return L; }
        public double getA() { return a; }
        public double getB() { return b; }
        public int getR() { return r; }
        public int getG() { return g; }
        public int getBl() { return bl; }

        // Конвертация из RGB в LAB
        public static ColorLab fromRGB(int r, int g, int b) {
            double[] xyz = rgbToXyz(r, g, b);
            double[] lab = xyzToLab(xyz[0], xyz[1], xyz[2]);
            return new ColorLab(lab[0], lab[1], lab[2], r, g, b);
        }

        // Расстояние в LAB-пространстве
        public double distance(ColorLab other) {
            double dL = this.L - other.L;
            double da = this.a - other.a;
            double db = this.b - other.b;
            return Math.sqrt(dL * dL + da * da + db * db);
        }

        // Конвертация RGB -> XYZ
        private static double[] rgbToXyz(int r, int g, int b) {
            double R = r / 255.0;
            double G = g / 255.0;
            double B = b / 255.0;

            R = (R > 0.04045) ? Math.pow((R + 0.055) / 1.055, 2.4) : R / 12.92;
            G = (G > 0.04045) ? Math.pow((G + 0.055) / 1.055, 2.4) : G / 12.92;
            B = (B > 0.04045) ? Math.pow((B + 0.055) / 1.055, 2.4) : B / 12.92;

            double X = R * 0.4124 + G * 0.3576 + B * 0.1805;
            double Y = R * 0.2126 + G * 0.7152 + B * 0.0722;
            double Z = R * 0.0193 + G * 0.1192 + B * 0.9505;

            return new double[]{X, Y, Z};
        }

        // Конвертация XYZ -> LAB
        private static double[] xyzToLab(double X, double Y, double Z) {
            double Xn = 0.95047, Yn = 1.00000, Zn = 1.08883;

            double x = fLab(X / Xn);
            double y = fLab(Y / Yn);
            double z = fLab(Z / Zn);

            double L = 116 * y - 16;
            double a = 500 * (x - y);
            double b = 200 * (y - z);

            return new double[]{L, a, b};
        }

        private static double fLab(double t) {
            return (t > 0.008856) ? Math.cbrt(t) : (7.787 * t + 16.0 / 116.0);
        }
    }

    // Усреднение массива цветов LAB
    private ColorLab averageColorLab(ColorLab[] colors) {
        double sumL = 0, suma = 0, sumb = 0;
        for (ColorLab c : colors) {
            sumL += c.getL();
            suma += c.getA();
            sumb += c.getB();
        }
        int n = colors.length;
        ColorLab first = colors[0];
        return new ColorLab(sumL / n, suma / n, sumb / n, first.getR(), first.getG(), first.getBl());
    }

    public Color findMostContrastingColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int quarterHeight = height / 4;
        int quarterWidth = width / 4;

        ColorLab[] topColors = new ColorLab[quarterHeight * width];
        int idx = 0;
        for (int y = 0; y < quarterHeight; y++) {
            for (int x = 0; x < width; x++) {
                java.awt.Color awtColor = new java.awt.Color(image.getRGB(x, y));
                topColors[idx++] = ColorLab.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            }
        }

        ColorLab[] leftColors = new ColorLab[height * quarterWidth];
        idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < quarterWidth; x++) {
                java.awt.Color awtColor = new java.awt.Color(image.getRGB(x, y));
                leftColors[idx++] = ColorLab.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            }
        }

        ColorLab[] rightColors = new ColorLab[height * quarterWidth];
        idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = width - quarterWidth; x < width; x++) {
                java.awt.Color awtColor = new java.awt.Color(image.getRGB(x, y));
                rightColors[idx++] = ColorLab.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
            }
        }

        ColorLab avgTop = averageColorLab(topColors);
        ColorLab avgLeft = averageColorLab(leftColors);
        ColorLab avgRight = averageColorLab(rightColors);

        ColorLab reference = new ColorLab(
                (avgTop.getL() + avgLeft.getL() + avgRight.getL()) / 3,
                (avgTop.getA() + avgLeft.getA() + avgRight.getA()) / 3,
                (avgTop.getB() + avgLeft.getB() + avgRight.getB()) / 3,
                (avgTop.getR() + avgLeft.getR() + avgRight.getR()) / 3,
                (avgTop.getG() + avgLeft.getG() + avgRight.getG()) / 3,
                (avgTop.getBl() + avgLeft.getBl() + avgRight.getBl()) / 3
        );

        double maxDistance = -1;
        ColorLab maxColor = null;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                java.awt.Color awtColor = new java.awt.Color(image.getRGB(x, y));
                ColorLab lab = ColorLab.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                double dist = lab.distance(reference);
                if (dist > maxDistance) {
                    maxDistance = dist;
                    maxColor = lab;
                }
            }
        }

        return Color.color(
                clamp01(maxColor.getR() / 255.0),
                clamp01(maxColor.getG() / 255.0),
                clamp01(maxColor.getBl() / 255.0)
        );
    }

    private double clamp01(double val) {
        return Math.min(1.0, Math.max(0.0, val));
    }
}
