package rf.ebanina.Network;

import javafx.scene.image.Image;

public interface IParseAlbumArt {
    Image getAlbumArt(String view, int height, int width, boolean isPreserveRatio, boolean isSmooth);
}
