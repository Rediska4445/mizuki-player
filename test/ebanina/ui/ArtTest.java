package ebanina.ui;

import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import rf.ebanina.UI.UI.Element.Art;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class ArtTest {

    @Test
    public void testSetAndGetImage() {
        Image mockImage = Mockito.mock(Image.class);

        when(mockImage.getProgress()).thenReturn(1.0);

        Art art = new Art(10);
        art.setImage(mockImage);

        assertSame(mockImage, art.getImage(), "getImage должен вернуть установленный мок-объект");
        assertSame(mockImage, art.getPreviousImage(), "getPreviousImage должен вернуть текущее изображение");
    }

    @Test
    public void testGetPreviousImage() {
        Image image1 = Mockito.mock(Image.class);
        Image image2 = Mockito.mock(Image.class);

        when(image1.getProgress()).thenReturn(1.0);
        when(image2.getProgress()).thenReturn(1.0);

        Art art = new Art(10);

        art.setImage(image1);
        art.setImage(image2);

        assertSame(image2, art.getImage(), "getImage должен вернуть последнее установленное изображение");
        assertSame(image1, art.getPreviousImage(), "getPreviousImage должен вернуть предыдущее изображение");
    }
}