package rf.ebanina.UI.UI.Element;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.CacheHint;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

public class Art extends Rectangle {
    private SimpleObjectProperty<Image> image = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Image> previousImage = new SimpleObjectProperty<>();

    public Art(int corners) {
        setCacheHint(CacheHint.QUALITY);
        setArcHeight(corners);
        setArcWidth(corners);
    }

    public void setImage(Image image) {
        if(this.image == null) {
            return;
        }

        previousImage.set(this.image.get());

        this.image.set(image);
        setFill(new ImagePattern(image));
    }

    public Image getPreviousImage() {
        return previousImage.get() == null ? image.get() : previousImage.get();
    }

    public Image getImage() {
        return image.get();
    }

    public SimpleObjectProperty<Image> imageProperty() {
        return image;
    }

    public SimpleObjectProperty<Image> previousImageProperty() {
        return previousImage;
    }

    public void setPreviousImage(Image previousImage) {
        this.previousImage.set(previousImage);
    }
}