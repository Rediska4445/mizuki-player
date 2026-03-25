package rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.util.Duration;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Context.Menu.Playlist.SimilarContextMenu;
import rf.ebanina.UI.UI.Context.Menu.Playlist.TrackContextMenu;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.concurrency.PriorityThreadFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// FIXME: Эта хуйня потребляет память ОЗУ
// FIXME: Эта хуйня лагает по CPU
// FIXME: Эта хуйня иногда не прогружает фон (не из за отмены задач и ограничения пула)
public class ListCellTrack<T>
        extends AnimatedListCell<Track>
{
    protected Label mainLabelOfTrack;

    private volatile Track current = null;

    private static final int CORE_POOL_SIZE_DATA = Runtime.getRuntime().availableProcessors();
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int BG_POOL_CORE = Math.max(4, CPU_COUNT);
    private static final int BG_POOL_MAX = Math.max(8, CPU_COUNT * 2);

    private static final ExecutorService dataLoadService = new ThreadPoolExecutor(
            CORE_POOL_SIZE_DATA,
            CORE_POOL_SIZE_DATA,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100),
            new PriorityThreadFactory(
                    "cell-data-loader",
                    Thread.NORM_PRIORITY + 1,
                    true
            ),
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    private static final ExecutorService backgroundAlbumArtService = new ThreadPoolExecutor(
            BG_POOL_CORE,
            BG_POOL_MAX,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new PriorityThreadFactory(
                    "cell-bg-art-loader",
                    Thread.NORM_PRIORITY - 1,
                    true
            ),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

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

        // Поставить дефолтные эффекты и данные для нормального вида
        pane.setOpacity(1.0);
        mainLabelOfTrack.setText(null);
        shadow.setColor(Color.TRANSPARENT);

        // Нужно для проверки на текущую ячейку
        current = item;

        // Путь к треку
        String path = item.getPath();

        // Если паттерн уже кэширован - поставить
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

    protected void loadDataAsync(Track item) {
        super.currentBgTask = backgroundAlbumArtService.submit(() -> {
            ImagePattern cachedPattern;
            synchronized (patternsCache) {
                cachedPattern = patternsCache.get(item.getPath());
            }

            if (cachedPattern == null) {
                Image img1 = item.getAlbumArt();

                if (img1 != null) {
                    cachedPattern = new ImagePattern(img1, 0, img1.getHeight() * 0.7, Math.min(getWidth(), img1.getWidth()), img1.getHeight(), false);

                    synchronized (patternsCache) {
                        patternsCache.put(item.getPath(), cachedPattern);
                    }
                }
            }

            if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                ImagePattern finalCachedPattern = cachedPattern;

                Platform.runLater(() -> {
                    super.setBackgroundImageCentered(finalCachedPattern, background);
                });
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
        });
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
        pt.play();
    }

    protected String getTopLabelText(Track item) {
        return Track.getFormattedTotalDuration(Float.parseFloat(item.getLastTimeTrack())) +
                " / " +
                Track.getFormattedTotalDuration(item.getTotalDurationInSeconds()) +
                " " +
                item.viewName();
    }

    @Override
    protected Node createExtraInfoContent() {
        return new VBox(3);
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