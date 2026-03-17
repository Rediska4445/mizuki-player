package rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.util.Duration;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Context.Menu.Playlist.SimilarContextMenu;
import rf.ebanina.UI.UI.Context.Menu.Playlist.TrackContextMenu;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.size;

// FIXME: Эта хуйня потребляет память ОЗУ
// FIXME: Эта хуйня лагает по CPU
public class ListCellTrack<T>
        extends AnimatedListCell<Track>
{
    protected Label mainLabelOfTrack;

    private volatile Track current = null;

    private static final ExecutorService dataLoadService = Executors.newFixedThreadPool(2);
    private static final ExecutorService backgroundAlbumArtService = Executors.newFixedThreadPool(2);

    private static final AtomicInteger albumArtCounter = new AtomicInteger(0);

    public ListCellTrack() {
        this(ColorProcessor.core.getMainClr());
    }

    public ListCellTrack(Color color) {
        super(color);

        pane = createBackgroundPane(28);
        pane.setLayoutY(getLayoutY());
        pane.setLayoutX(getLayoutX());
        pane.toFront();
        pane.setCache(true);
        pane.setCacheHint(CacheHint.SPEED);

        initCoverIcon();

        cover.setEffect(shadow);

        mainLabelOfTrack = new Label();
        mainLabelOfTrack.setFont(ResourceManager.Instance.loadFont("main_font", 12));
        mainLabelOfTrack.setLayoutY(2.5);
        mainLabelOfTrack.setLayoutX(cover.getWidth() + 10);

        pane.getChildren().addAll(cover, mainLabelOfTrack);

        setText(null);
    }

    @Override
    protected void updateItem(Track item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);

            return;
        }

        pane.setOpacity(1.0);
        mainLabelOfTrack.setText(null);
        shadow.setColor(Color.TRANSPARENT);

        current = item;

        String path = item.getPath();

        ImagePattern cachedThumb = patternsMipmapCache.get(path);
        Color cachedColor = colorsCache.get(path);

        if (cachedThumb != null) {
            cover.setFill(cachedThumb);

            if (cachedColor != null) {
                mainLabelOfTrack.setTextFill(cachedColor);
                shadow.setColor(cachedColor);
            }
        } else {
            cover.setFill(ColorProcessor.logoPattern);
        }

        loadDataAsync(item, mainLabelOfTrack);

        if (getContextMenu() == null || !getContextMenu().getUserData().equals(item)) {
            var menu = item.isNetty() ? new SimilarContextMenu(item) : new TrackContextMenu(item, this);
            menu.setUserData(item);

            setContextMenu(menu);
        }

        if (item.isPhantom())
            pane.setOpacity(0.25);

        setGraphic(pane);
    }

    private void loadDataAsync(Track item, Label label) {
        super.currentBgTask = backgroundAlbumArtService.submit(() -> {
            ImagePattern mipmapCachedPattern;
            synchronized (patternsCache) {
                mipmapCachedPattern = patternsCache.get(item.getPath());
            }

            if (mipmapCachedPattern == null) {
                Image img1 = !item.isNetty() ? item.getAlbumArt() : null;

                if (img1 != null) {
                    mipmapCachedPattern = new ImagePattern(img1, 0, img1.getHeight() * 0.7, Math.min(getWidth(), img1.getWidth()), img1.getHeight(), false);

                    synchronized (patternsCache) {
                        patternsCache.put(item.getPath(), mipmapCachedPattern);
                    }
                }
            }

            if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                ImagePattern finalMipmapCachedPattern = mipmapCachedPattern;

                Platform.runLater(() -> super.setBackgroundImageCentered(finalMipmapCachedPattern, background));
            }
        });

        super.currentTask = dataLoadService.submit(() -> {
            String topLabelText = getTopLabelText(item);

            if(albumArtCounter.addAndGet(1) > Track.CACHE_SIZE) {
                Track.albumArtCleaner(getListView().getItems());

                albumArtCounter.set(0);
            }

            ImagePattern mipmapCachedPattern;
            synchronized (patternsMipmapCache) {
                mipmapCachedPattern = patternsMipmapCache.get(item.getPath());
            }

            if (mipmapCachedPattern == null) {
                Image thumb = item.getIndependentMipmap(40);

                if (thumb != null) {
                    mipmapCachedPattern = new ImagePattern(thumb);

                    synchronized (patternsMipmapCache) {
                        patternsMipmapCache.put(item.getPath(), mipmapCachedPattern);
                    }
                }
            }

            Color colorRes = colorsCache.get(item.getPath());

            if (colorRes == null) {
                Image imgMipMap = mipmapCachedPattern.getImage();

                if(imgMipMap != null)
                    colorRes = ColorProcessor.core.getGeneralColorFromImage(imgMipMap);

                if (colorRes != null)
                    colorsCache.put(item.getPath(), colorRes);
            }

            final Color finalColor = colorRes;
            final ImagePattern finalMipmapCachedPattern = mipmapCachedPattern;

            Platform.runLater(() -> {
                if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                    animateDataArrival(finalColor, finalMipmapCachedPattern, topLabelText);
                }
            });

            if (item.isNetty() && item.equals(getItem()) && item.equals(current) && getItem().equals(current) && !isEmpty()) {
                item.setAlbumArt(item.getAlbumArt(size, size, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth));
            }
        });
    }

    protected void animateDataArrival(Color targetColor, ImagePattern targetPattern, String targetText) {
        if (targetText.equals(mainLabelOfTrack.getText()) && targetColor.equals(mainLabelOfTrack.getTextFill())) {
            return;
        }

        if (expansionTimeline != null) expansionTimeline.stop();

        pane.setOpacity(0.5);
        mainLabelOfTrack.setOpacity(0.0);
        cover.setScaleX(0.9);
        cover.setScaleY(0.9);

        mainLabelOfTrack.setText(targetText);
        mainLabelOfTrack.setTextFill(targetColor);
        cover.setFill(targetPattern);

        ParallelTransition pt = new ParallelTransition();

        FadeTransition fadePane = new FadeTransition(Duration.millis(300), pane);
        fadePane.setToValue(1.0);

        FadeTransition fadeLabel = new FadeTransition(Duration.millis(400), mainLabelOfTrack);
        fadeLabel.setToValue(1.0);

        ScaleTransition scaleCover = new ScaleTransition(Duration.millis(400), cover);
        scaleCover.setToX(1.0);
        scaleCover.setToY(1.0);
        scaleCover.setInterpolator(Interpolator.EASE_OUT);

        Timeline colorTimeline = new Timeline(
                new KeyFrame(Duration.millis(400),
                        new KeyValue(shadow.colorProperty(), targetColor, Interpolator.EASE_OUT)
                )
        );

        pt.getChildren().addAll(fadePane, fadeLabel, scaleCover, colorTimeline);
        pt.play();
    }

    private String getTopLabelText(Track item) {
        String fullInfo = item.metadata.get("fullInfoInCellTrack", String.class);

        if(fullInfo == null) {
            fullInfo = Track.getFormattedTotalDuration(Float.parseFloat(item.getLastTimeTrack())) +
                    " / " +
                    Track.getFormattedTotalDuration(item.getTotalDurationInSeconds()) +
                    " " +
                    item.viewName() +
                    " - " +
                    item.getExtension();

            item.metadata.put("fullInfoInCellTrack", fullInfo, String.class);
        }

        return fullInfo;
    }

    @Override
    protected Node createExtraInfoContent() {
        return new VBox(1);
    }

    @Override
    protected void onItemDropped(int draggedIndex, int targetIndex) {
        Track temp = getListView().getItems().get(draggedIndex);
        getListView().getItems().set(draggedIndex, this.getListView().getItems().get(targetIndex));
        getListView().getItems().set(targetIndex, temp);

        Track temp1 = PlayProcessor.playProcessor.getTracks().get(draggedIndex);
        PlayProcessor.playProcessor.getTracks().set(draggedIndex, PlayProcessor.playProcessor.getTracks().get(targetIndex));
        PlayProcessor.playProcessor.getTracks().set(targetIndex, temp1);

        getListView().getSelectionModel().select(targetIndex);
    }
}