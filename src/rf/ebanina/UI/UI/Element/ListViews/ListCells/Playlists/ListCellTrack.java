package rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.util.Duration;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Context.Menu.Playlist.SimilarContextMenu;
import rf.ebanina.UI.UI.Context.Menu.Playlist.TrackContextMenu;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Music;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.concurrency.PriorityThreadFactory;
import rf.ebanina.utils.loggining.logging;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.*;

// FIXME: Эта хуйня потребляет память ОЗУ
// FIXME: Эта хуйня лагает по CPU
// FIXME: Эту хуйня иногда неправильно прогружается
// блять я реально долбаёб
@logging(tag = "AnimatedListCell/ListCellTrack")
public class ListCellTrack<T>
        extends AnimatedListCell<Track>
{
    protected Label mainLabelOfTrack;

    // (1) Пул для “тяжёлых” загрузок: трек‑данные, длительные вычисления
    private static final ExecutorService dataLoadService = new ThreadPoolExecutor(
            2,
            4,
            30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(64),
            new PriorityThreadFactory("DataLoad", Thread.NORM_PRIORITY, true)
    );

    // (2) Отдельный пул для загрузки/обработки картинок (альбом‑арт)
    private static final ExecutorService backgroundAlbumArtService = new ThreadPoolExecutor(
            2,
            4,
            30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(64),
            new PriorityThreadFactory("AlbumArt", Thread.NORM_PRIORITY, true)
    );

    private static final AtomicInteger albumArtCounter = new AtomicInteger(0);

    public ListCellTrack() {
        super();

        pane = createBackgroundPane(28);
        pane.setLayoutY(getLayoutY());
        pane.setLayoutX(getLayoutX());
        pane.toFront();

        initCoverIcon();

        cover.setEffect(shadow);

        mainLabelOfTrack = new Label();
        mainLabelOfTrack.setFont(ResourceManager.Instance.loadFont("main_font", 12));
        mainLabelOfTrack.setLayoutY(2.5);
        mainLabelOfTrack.setLayoutX(cover.getWidth() + 10);

        pane.getChildren().addAll(cover, mainLabelOfTrack);
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
        mainLabelOfTrack.setText(item.viewName);
        shadow.setColor(Color.TRANSPARENT);

        loadDataAsync(item);

        if (getContextMenu() == null || !getContextMenu().getUserData().equals(item)) {
            var menu = item.isNetty() ? new SimilarContextMenu(item) : new TrackContextMenu(item, this);
            menu.setUserData(item);

            setContextMenu(menu);
        }

        if (item.isPhantom())
            pane.setOpacity(0.25);

        setGraphic(pane);
    }

    private Runnable loadBackgroundLocalTrack(Track item) {
        return () -> {
            Image img1 = item.getAlbumArt();

            if (isActive(item, currentBgTask)) {
                Platform.runLater(() -> setBackgroundImageCentered(img1, getWidth(), background));
            }
        };
    }

    private Runnable loadBackgroundNettyTrack(Track item) {
        return () -> {
            try {
                String url = item.metadata.get("album_art", String.class);

                Image buff;

                if (url != null) {
                    buff = item.albumArt == null ? item
                            .setAlbumArt(new Image(item.metadata.get("album_art", String.class), size, size, isPreserveRatio, isSmooth)).getAlbumArt()
                            : item.getAlbumArt();
                } else {
                    buff = null;
                }

                if (buff != null) {
                    if (super.isActive(item, currentBgTask)) {
                        Platform.runLater(() -> setBackgroundImageCentered(buff, getWidth(), background));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    private Runnable loadDataLocalTrack(Track item) {
        return () -> {
            String topLabelText = getTopLabelText(item);

            if(albumArtCounter.addAndGet(1) > Track.CACHE_SIZE) {
                Track.albumArtCleaner(getListView().getItems());

                albumArtCounter.set(0);
            }

            Image buff = item.getMipmap(40);

            Color colorRes = ColorProcessor.core.getGeneralColorFromImage(buff);

            Platform.runLater(() -> {
                if (isActive(item, currentTask)) {
                    animateDataArrival(colorRes, new ImagePattern(buff), topLabelText);
                }
            });
        };
    }

    private Runnable loadDataNettyTrack(Track item) {
        return () -> {
            try {
                Image buff = item.mipmap;

                if(item.getProperties().get("mipmap_is_loaded", boolean.class) == null) {
                    item.getProperties().put("mipmap_is_loaded", false, boolean.class);
                }

                if(buff == null || !item.getProperties().get("mipmap_is_loaded", boolean.class)) {
                    if(item.getProperties().get("album_art", String.class) == null) {
                        item.getProperties().put("album_art", ColorProcessor.getCore().getLogo().getUrl(), String.class);
                    }

                    item.setMipmap(Track.createMipmap(item.getProperties().get("album_art", String.class))).getMipmap();

                    item.getProperties().put("mipmap_is_loaded", true, boolean.class);
                }

                Color color = ColorProcessor.core.getGeneralColorFromImage(buff);

                String viewName = item.viewName();

                Platform.runLater(() -> {
                    if (isActive(item, currentTask)) {
                        cover.setFill(new ImagePattern(buff != null ? buff : logo));
                        shadow.setColor(color);
                        mainLabelOfTrack.setTextFill(color);
                        mainLabelOfTrack.setText(viewName + " / " + item);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    protected void loadDataAsync(Track item) {
        try {
            super.currentBgTask = backgroundAlbumArtService.submit(item.isNetty() ? loadBackgroundNettyTrack(item) : loadBackgroundLocalTrack(item));
            super.currentTask = dataLoadService.submit(item.isNetty() ? loadDataNettyTrack(item) : loadDataLocalTrack(item));
        } catch (RejectedExecutionException e) {
            Music.mainLogger.warn("ThreadPool overloaded, skip async load for track: " + item);
        }
    }

    protected void animateDataArrival(Color targetColor, ImagePattern targetPattern, String targetText) {
        if (targetText.equals(mainLabelOfTrack.getText()) && targetColor.equals(mainLabelOfTrack.getTextFill())) {
            return;
        }

        if (expansionTimeline != null)
            expansionTimeline.stop();

        pane.setOpacity(0.5);
        mainLabelOfTrack.setOpacity(0.0);
        cover.setScaleX(0.9);
        cover.setScaleY(0.9);

        mainLabelOfTrack.setText(targetText);
        mainLabelOfTrack.setTextFill(targetColor);
        cover.setFill(targetPattern);

        buildAnimationDataArrival(pane, mainLabelOfTrack, cover, targetColor).play();
    }

    protected ParallelTransition buildAnimationDataArrival(Node pane, Node mainLabelOfTrack, Node cover, Color targetColor) {
        ParallelTransition pt = new ParallelTransition();

        FadeTransition fadePane = new FadeTransition(Duration.millis(300), pane);
        fadePane.setToValue(1.0);

        FadeTransition fadeLabel = new FadeTransition(Duration.millis(400), mainLabelOfTrack);
        fadeLabel.setToValue(1.0);

        ScaleTransition scaleCover = new ScaleTransition(Duration.millis(400), cover);
        scaleCover.setToX(1.0);
        scaleCover.setToY(1.0);
        scaleCover.setInterpolator(Root.iceInterpolator);

        Timeline colorTimeline = new Timeline(
                new KeyFrame(Duration.millis(400),
                        new KeyValue(shadow.colorProperty(), targetColor, Root.iceInterpolator)
                )
        );

        pt.getChildren().addAll(fadePane, fadeLabel, scaleCover, colorTimeline);

        return pt;
    }

    private String getTopLabelText(Track item) {
        return Track.getFormattedTotalDuration(Float.parseFloat(item.getLastTimeTrack())) +
                " / " +
                Track.getFormattedTotalDuration(item.getTotalDurationInSeconds()) +
                " " +
                item.viewName();
    }

    @Override
    protected Node createExtraInfoContent() {
        return null;
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