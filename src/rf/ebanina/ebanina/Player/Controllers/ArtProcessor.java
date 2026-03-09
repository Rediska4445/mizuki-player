package rf.ebanina.ebanina.Player.Controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Animations;
import rf.ebanina.UI.UI.Element.Buttons.Button;
import rf.ebanina.UI.UI.Element.ListViews.Playlist.PlayView;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rf.ebanina.UI.Root.*;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.*;
import static rf.ebanina.UI.Root.PlaylistHandler.playlistSelected;

public class ArtProcessor
        implements IArtProcessor
{
    public Duration durationIn = Duration.millis(250);
    public Duration durationOut = Duration.millis(250);
    public final Duration animateColorChangeDura = Duration.millis(250);

    public double scaleOutTarget = 0.7;
    public double scaleInStart = 0.7;
    public double scaleInEnd = 1.0;

    public Interpolator acceleratingInterpolator = Interpolator.SPLINE(0.0, 0.0, 0.4, 1.0);

    protected ParallelTransition outTransition;
    protected ParallelTransition inTransition;
    protected Animation inDropShadowAnimation;
    protected Animation outDropShadowAnimation;

    private final Object imageLock = new Object();

    @Override
    public void updateColors(Color color) {
        updateTextsColors(color);
        updateCheckBoxColors(color);
        updateButtonsColors(color);
    }

    public void updateButtonsColors(Color color) {
        btn.setColorIconHover(color);
        btnNext.setColorIconHover(color);
        btnDown.setColorIconHover(color);

        btn.setColorBgPressed(color);
        btnNext.setColorBgPressed(color);
        btnDown.setColorBgPressed(color);

        tracksListView.getBtnPlaylist().setColorIconHover(color);
        ((Button) tracksListView.getBtnPlaylistNext()).setColorIconHover(color);
        ((Button) tracksListView.getBtnPlaylistDown()).setColorIconHover(color);

        tracksListView.getBtnPlaylist().setColorBgPressed(color);
        ((Button) tracksListView.getBtnPlaylistNext()).setColorBgPressed(color);
        ((Button) tracksListView.getBtnPlaylistDown()).setColorBgPressed(color);

        ((Button) similar.getBtnPlaylist()).setColorIconHover(color);
        ((Button) similar.getBtnPlaylistNext()).setColorIconHover(color);
        ((Button) similar.getBtnPlaylistDown()).setColorIconHover(color);

        ((Button) similar.getBtnPlaylist()).setColorBgPressed(color);
        ((Button) similar.getBtnPlaylistNext()).setColorBgPressed(color);
        ((Button) similar.getBtnPlaylistDown()).setColorBgPressed(color);

        Root.mainFunctions.getMainButton().setColorIconHover(color);
        Root.mainFunctions.getMainButton().setColorBgPressed(color);

        ((Button) hideControlLeft).setColorIconHover(color);
        ((Button) hideControlRight).setColorIconHover(color);

        ((Button) hideControlLeft).setColorBgPressed(color);
        ((Button) hideControlRight).setColorBgPressed(color);
    }

    public void updateCheckBoxColors(Color color) {
        if(!stage.isFocused()) {
            tracksListView.getBtnPlaylist().setTextFill(color);
        } else {
            animateColorChange(tracksListView.getBtnPlaylist().getTextFill(), color, tracksListView.getBtnPlaylist().textFillProperty());
        }
    }

    // FIXME: Перевести в Animations класс для унифицированного контроля
    private void updateTextsColors(Color color) {
        if(!stage.isFocused()) {
            currentArtist.updateColor(color);
            currentTrackName.updateColor(color);
            beginTime.updateColor(color);
            endTime.updateColor(color);
            soundSlider.setColor(color);

            tracksListView.getCurrentPlaylistText().updateColor(color);
            tracksListView.getSearchBar().setUnFocusColor(color);

            similar.getCurrentPlaylistText().updateColor(color);
            similar.getSearchBar().setUnFocusColor(color);
        } else {
            animateColorChange(soundSlider.getColorProperty().get(), color, soundSlider.getColorProperty());
            animateColorChange(currentArtist.getColorProperty().get(), color, currentArtist.getColorProperty());
            animateColorChange(currentTrackName.getColorProperty().get(), color, currentTrackName.getColorProperty());
            animateColorChange(beginTime.getColorProperty().get(), color, beginTime.getColorProperty());
            animateColorChange(endTime.getColorProperty().get(), color, endTime.getColorProperty());

            animateColorChange(tracksListView.getSearchBar().getColorProperty().get(), color, tracksListView.getSearchBar().getColorProperty());
            animateColorChange(tracksListView.getSearchBar().getUnFocusColor(), color, tracksListView.getSearchBar().unFocusColorProperty());
            animateColorChange(tracksListView.getCurrentPlaylistText().getColorProperty().get(), color, tracksListView.getCurrentPlaylistText().getColorProperty());

            animateColorChange(similar.getSearchBar().getColorProperty().get(), color, similar.getSearchBar().getColorProperty());
            animateColorChange(similar.getSearchBar().getUnFocusColor(), color, similar.getSearchBar().unFocusColorProperty());
            animateColorChange(similar.getCurrentPlaylistText().getColorProperty().get(), color, similar.getCurrentPlaylistText().getColorProperty());
        }
    }

    protected Timeline animateColorChange(Color startColor, Color endColor, ObjectProperty<Color> colorProperty) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(colorProperty, startColor)),
                new KeyFrame(animateColorChangeDura, new KeyValue(colorProperty, endColor))
        );

        timeline.setOnFinished((e) -> colorProperty.set(endColor));

        Platform.runLater(timeline::play);

        return timeline;
    }

    protected void animateColorChange(Paint startColor, Paint endColor, ObjectProperty<Paint> colorProperty) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(colorProperty, startColor)),
                new KeyFrame(animateColorChangeDura, new KeyValue(colorProperty, endColor))
        );

        timeline.play();
    }

    public void setRootColor(Pane pane, Color r, Image image) {
        setRootColor(pane, r, /* previousImageArt */ art != null ? art.getPreviousImage() : image, image);
    }

    public void setRootColor(Pane pane, Color r, Image previous, Image image) {
        pane.setBackground(new Background(new BackgroundFill(r, CornerRadii.EMPTY, Insets.EMPTY)));

        background.setImage(image);
        background_under.setImage(previous);

        if(background_under.getImage() != null && background.getImage() != null) {
            background.setOpacity(0);
            background_under.setOpacity(1);

            Timeline fadeIn = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(background.opacityProperty(), 0)),
                    new KeyFrame(durationIn, new KeyValue(background.opacityProperty(), 1.0))
            );

            Timeline fadeOut = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(background_under.opacityProperty(), 1.0)),
                    new KeyFrame(durationIn, new KeyValue(background_under.opacityProperty(), 0))
            );

            ParallelTransition parallelTransition = new ParallelTransition(fadeIn, fadeOut);
            parallelTransition.play();
        }
    }

    public Timeline dropShadowColor(Color c) {
        Timeline timeline = new Timeline(
                new KeyFrame(durationIn,
                        new KeyValue(imgTrackShadow.colorProperty(), c)
                )
        );

        timeline.setCycleCount(1);
        timeline.setAutoReverse(false);

        return timeline;
    }

    protected final ExecutorService service = Executors.newSingleThreadExecutor();

    public void initColor(Image image) {
        service.submit(() -> {
            synchronized (imageLock) {
                Color newColor = ColorProcessor.core.getGeneralColorFromImage(image);

                Platform.runLater(() -> {
                    ColorProcessor.core.setMainClr(newColor);

                    Animations.play(dropShadowColor(newColor), "dropShadowColor", () -> imgTrackShadow.setColor(newColor));

                    if (ConfigurationManager.instance.getBooleanItem("rainbow", "true")) {
                        updateColors(newColor);

                        initListViews();
                    }

                    if (ConfigurationManager.instance.getBooleanItem("is_blur_background", "true")) {
                        setRootColor(root, ColorProcessor.core.getMainClr(), image);
                    }

                    Root.rootImpl.setStageCaptionColor(stage, newColor);
                });
            }
        });
    }

    private Animation dropShadowTimeLine(Node node, int dura, int newProperties, Interpolator interpolator) {
        DropShadow shadow = (DropShadow) node.getEffect();
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(shadow.widthProperty(), shadow.getWidth(), interpolator),
                        new KeyValue(shadow.heightProperty(), shadow.getHeight(), interpolator)
                ),
                new KeyFrame(Duration.millis(dura),
                        new KeyValue(shadow.widthProperty(), newProperties, interpolator),
                        new KeyValue(shadow.heightProperty(), newProperties, interpolator)
                )
        );

        return timeline;
    }

    public void setIcon() {
        stage.getIcons().setAll(!stage.isFocused() ? art.getImage() : logo);
        stage.setTitle(!stage.isFocused() ? currentArtist.getText() + " - " + currentTrackName.getText() : Music.name);
    }

    @Override
    public void initArt(Track track) {
        if (outTransition != null)
            outTransition.stop();

        if (inTransition != null)
            inTransition.stop();

        if (inDropShadowAnimation != null)
            inDropShadowAnimation.stop();

        if (outDropShadowAnimation != null)
            outDropShadowAnimation.stop();

        //previousImageArt = art.getImage();

        double width = art.getBoundsInParent().getWidth();
        double height = art.getBoundsInParent().getHeight();

        boolean goLeftOrDown = PlayProcessor.playProcessor.getPreviousIndex() < PlayProcessor.playProcessor.getTrackIter();

        double translateOutFromX;
        double translateOutFromY;
        double translateOutToX;
        double translateOutToY;

        double translateInFromX;
        double translateInFromY;
        double translateInToX;
        double translateInToY;

        // Горизонтальное направление
        if (!playlistSelected) {
            if (goLeftOrDown) {
                translateOutToX = -width;
                translateInFromX = width;
            } else {
                translateOutToX = width;
                translateInFromX = -width;
            }

            translateOutToY = 0;
            translateInFromY = 0;
        } else {
            // Вертикальное направление
            if (goLeftOrDown) {
                translateOutToY = -height;
                translateInFromY = height;
            } else {
                translateOutToY = height;
                translateInFromY = -height;
            }

            translateOutToX = 0;
            translateInFromX = 0;
        }

        translateOutFromX = 0;
        translateInToX = 0;
        translateOutFromY = 0;
        translateInToY = 0;

        TranslateTransition moveOut = new TranslateTransition(durationIn, art);
        moveOut.setFromX(translateOutFromX);
        moveOut.setToX(translateOutToX);
        moveOut.setFromY(translateOutFromY);
        moveOut.setToY(translateOutToY);
        moveOut.setInterpolator(acceleratingInterpolator);

        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(art.opacityProperty(), 1, acceleratingInterpolator)),
                new KeyFrame(durationIn, new KeyValue(art.opacityProperty(), 0, acceleratingInterpolator))
        );

        Timeline scaleOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(art.scaleXProperty(), 1, acceleratingInterpolator),
                        new KeyValue(art.scaleYProperty(), 1, acceleratingInterpolator)),
                new KeyFrame(durationIn,
                        new KeyValue(art.scaleXProperty(), scaleOutTarget, acceleratingInterpolator),
                        new KeyValue(art.scaleYProperty(), scaleOutTarget, acceleratingInterpolator))
        );

        outTransition = new ParallelTransition(moveOut, fadeOut, scaleOut);

        inDropShadowAnimation = dropShadowTimeLine(
                art,
                250,
                0,
                general_interpolator
        );
        Animations.play(inDropShadowAnimation, "inDropShadowAnimation");

        final double finalTranslateInFromY = translateInFromY;
        final double finalTranslateInFromX = translateInFromX;
        final double finalTranslateInToX = translateInToX;
        final double finalTranslateInToY = translateInToY;

        outTransition.setOnFinished(actionEvent -> new Thread(() -> {
            synchronized (imageLock) {
                Image img = track.getAlbumArt();

                Platform.runLater(() -> {
                    setImage(img);
                    initColor(art.getImage());
                    setIcon();

                    art.setTranslateX(finalTranslateInFromX);
                    art.setTranslateY(finalTranslateInFromY);
                    art.setOpacity(0);
                    art.setScaleX(scaleInStart);
                    art.setScaleY(scaleInStart);

                    outDropShadowAnimation = dropShadowTimeLine(
                            art,
                            250,
                            size,
                            general_interpolator
                    );

                    Animations.play(outDropShadowAnimation, "outDropShadowAnimation", () -> {
                        if(art.getEffect() instanceof DropShadow dropShadow) {
                            dropShadow.setWidth(size);
                            dropShadow.setHeight(size);
                        }
                    });

                    TranslateTransition moveIn = new TranslateTransition(durationOut, art);
                    moveIn.setFromX(finalTranslateInFromX);
                    moveIn.setToX(finalTranslateInToX);
                    moveIn.setFromY(finalTranslateInFromY);
                    moveIn.setToY(finalTranslateInToY);
                    moveIn.setInterpolator(acceleratingInterpolator);

                    FadeTransition fadeIn = new FadeTransition(durationOut, art);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.setInterpolator(acceleratingInterpolator);

                    Timeline scaleIn = new Timeline(
                            new KeyFrame(Duration.ZERO,
                                    new KeyValue(art.scaleXProperty(), scaleInStart, acceleratingInterpolator),
                                    new KeyValue(art.scaleYProperty(), scaleInStart, acceleratingInterpolator)),
                            new KeyFrame(durationOut,
                                    new KeyValue(art.scaleXProperty(), scaleInEnd, acceleratingInterpolator),
                                    new KeyValue(art.scaleYProperty(), scaleInEnd, acceleratingInterpolator))
                    );

                    inTransition = new ParallelTransition(moveIn, fadeIn, scaleIn);
                    Animations.play(inTransition, "inTransition", () -> {
                        art.setTranslateX(0);
                        art.setTranslateY(0);
                        art.setOpacity(1);
                        art.setScaleX(scaleInEnd);
                        art.setScaleY(scaleInEnd);
                    });
                });
            }
        }).start());

        if (playlistSelected)
            playlistSelected = false;

        Animations.play(outTransition, "outTransition", () -> new Thread(() -> {
            synchronized (imageLock) {
                Image img = track.getAlbumArt();

                Platform.runLater(() -> {
                    art.setTranslateX(0);
                    art.setTranslateY(0);
                    art.setOpacity(1);
                    art.setScaleX(scaleInEnd);
                    art.setScaleY(scaleInEnd);

                    setImage(img);
                    initColor(art.getImage());
                    setIcon();
                });
            }
        }).start());
    }

    public void setImage(Image img) {
        if (ColorProcessor.core.getHue() != 0)
            img = ColorProcessor.core.changeHue(img, ConfigurationManager.instance.getBooleanItem("is_hue_change", "true") ? ColorProcessor.core.getHue() : 0);

        art.setImage(img);
    }

    public void initListViews() {
          for(Node n : root.getChildren()) {
              if(n instanceof PlayView<?, ?> listView) {
                  listView.getTrackListView().updateSelectedBackground(core.getMainClr());
                  listView.getTrackListView().updateBorderColor(core.getMainClr());

                  listView.getPlaylistListView().updateSelectedBackground(core.getMainClr());
                  listView.getPlaylistListView().updateBorderColor(core.getMainClr());
              }
          }
    }
}