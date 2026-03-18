package rf.ebanina.UI.UI.Popup;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Text.Label;

import static rf.ebanina.UI.Root.corners;

public class Preview
        extends Popup
{
    private javafx.scene.layout.BorderPane top;
    private javafx.scene.image.Image image;
    private Label artist;
    private Label track;
    private Label time;
    private javafx.scene.shape.Rectangle imageCornest;

    public Preview() {
        super(new Pane());
    }

    public Preview Build(double x, double y, javafx.scene.image.Image image1, String artist1, String title1, Color color) {
        pane.getChildren().clear();

        pane.setLayoutX(x);
        pane.setLayoutY(y);

        pane.setPrefHeight(175);
        pane.setPrefWidth(125);
        pane.setDisable(true);
        pane.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.6),
                new CornerRadii(10),
                new Insets(0, 0, 0, 0))
        ));

        setImage(image1);

        imageCornest = new javafx.scene.shape.Rectangle();
        imageCornest.setFill(new ImagePattern(image1));
        imageCornest.setWidth(100);
        imageCornest.setHeight(100);
        imageCornest.setArcWidth(corners);
        imageCornest.setArcHeight(corners);
        imageCornest.setLayoutX((pane.getPrefWidth() - imageCornest.getWidth()) / 2);
        imageCornest.setLayoutY(50);

        DropShadow dropShadow = new DropShadow(30, color);
        imageCornest.setEffect(dropShadow);
        imageCornest.setCache(true);

        track = new Label(title1);
        track.setOpacity(1);
        track.setTextFill(color);
        track.toFront();
        track.setFont(ResourceManager.Instance.loadFont("main_font", 13));
        track.setMaxWidth(pane.getPrefWidth() - 15);
        track.setAlignment(Pos.CENTER);

        artist = new Label(artist1);
        artist.setOpacity(1);
        artist.toFront();
        artist.setTextFill(color);
        artist.setFont(ResourceManager.Instance.loadFont("font", 12));
        artist.setMaxWidth(pane.getPrefWidth() - 15);
        artist.setAlignment(Pos.CENTER);

        time = new Label();
        time.setLayoutX(0);
        time.setTextFill(color);
        time.setLayoutY(imageCornest.getLayoutY() + 10);
        time.setAlignment(Pos.CENTER);

        top = new javafx.scene.layout.BorderPane();
        top.setTop(track);
        top.setCenter(artist);
        top.setPrefWidth(pane.getPrefWidth());
        top.setLayoutY((pane.getPrefHeight() / 2 - imageCornest.getLayoutY()) / 4);

        javafx.scene.layout.BorderPane.setAlignment(track, Pos.CENTER);
        javafx.scene.layout.BorderPane.setAlignment(artist, Pos.CENTER);

        pane.getChildren().addAll(top, imageCornest, time);

        return this;
    }

    public javafx.scene.shape.Rectangle getImageCornest() {
        return imageCornest;
    }

    public Preview setImageCornest(javafx.scene.shape.Rectangle imageCornest) {
        this.imageCornest = imageCornest;
        return this;
    }

    public Pane getPane() {
        return pane;
    }

    public void setPane(Pane pane) {
        this.pane = pane;
    }

    public javafx.scene.layout.BorderPane getTop() {
        return top;
    }

    public void setTop(javafx.scene.layout.BorderPane top) {
        this.top = top;
    }

    public javafx.scene.image.Image getImage() {
        return image;
    }

    public void setImage(javafx.scene.image.Image image) {
        this.image = image;
    }

    public Label getArtist() {
        return artist;
    }

    public void setArtist(Label artist) {
        this.artist = artist;
    }

    public Label getTrack() {
        return track;
    }

    public void setTrack(Label track) {
        this.track = track;
    }
}
