package rf.ebanina.Network;

import javafx.scene.image.Image;

@FunctionalInterface
public interface IParseAlbumArt {
    Image getAlbumArt(String view, int height, int width, boolean isPreserveRatio, boolean isSmooth);
}
