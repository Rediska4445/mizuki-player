package ebanina.gui;

import javafx.embed.swing.JFXPanel;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rf.ebanina.UI.UI.Element.Art;

import static org.junit.jupiter.api.Assertions.assertSame;

public class ArtTest {
    @BeforeAll
    public static void initJfx() {
        new JFXPanel();
    }

    @Test
    public void testSetAndGetImage() {
        Art art = new Art(10);
        Image image1 = new Image("https://via.placeholder.com/100");
        art.setImage(image1);

        assertSame(image1, art.getImage(), "getImage должен вернуть установленное изображение");
        assertSame(image1, art.getPreviousImage(), "getPreviousImage должен вернуть текущее изображение, если предыдущего нет");
    }

    @Test
    public void testGetPreviousImage() {
        Art art = new Art(10);
        Image image1 = new Image("https://via.placeholder.com/100");
        Image image2 = new Image("https://via.placeholder.com/200");

        art.setImage(image1);
        art.setImage(image2);

        assertSame(image2, art.getImage(), "getImage должен вернуть последнее установленное изображение");
        assertSame(image1, art.getPreviousImage(), "getPreviousImage должен вернуть предыдущее изображение");
    }
}