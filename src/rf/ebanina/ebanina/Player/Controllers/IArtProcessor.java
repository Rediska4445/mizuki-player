package rf.ebanina.ebanina.Player.Controllers;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import rf.ebanina.ebanina.Player.Track;

public interface IArtProcessor {
    void updateColors(Color color);
    void initColor(Image image);
    void initArt(Track path);
    void setImage(Image img);
}
