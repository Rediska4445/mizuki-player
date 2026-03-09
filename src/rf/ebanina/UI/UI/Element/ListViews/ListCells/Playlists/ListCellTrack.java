package rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import rf.ebanina.UI.UI.Context.Menu.Playlist.SimilarContextMenu;
import rf.ebanina.UI.UI.Context.Menu.Playlist.TrackContextMenu;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.size;

// FIXME: Эта хуйня потребляет память ОЗУ

public class ListCellTrack<T> extends AnimatedListCell<Track> {
    private final boolean saveFactoryTime;
    private final Image logo;

    private volatile Track current = null;

    private static final ExecutorService dataLoadService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final ExecutorService backgroundAlbumArtService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final AtomicInteger albumArtCounter = new AtomicInteger(0);

    public ListCellTrack(boolean saveFactoryTime, Image defaultLogo) {
        this(ColorProcessor.core.getMainClr(), saveFactoryTime, defaultLogo);
    }

    Label label;

    public ListCellTrack(Color color, boolean saveFactoryTime, Image defaultLogo) {
        super(color);

        this.saveFactoryTime = saveFactoryTime;
        this.logo = defaultLogo;

        pane = createBackgroundPane(28);

        if(getItem() != null) {
            final Image img = getItem().mipmap == null ? logo : getItem().mipmap;

            cover = setCoverIcon(img);
            cover.setEffect(shadow);

            label = new Label();
            label.setFont(ResourceManager.Instance.loadFont("main_font", 12));
            label.setLayoutY(2.5);
            label.setLayoutX(cover.getWidth() + 10);

            pane.getChildren().addAll(cover, label);
            setPadding(new Insets(0));
        }
    }

    @Override
    protected void updateItem(Track item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);

            return;
        }

        current = item;

        pane = createBackgroundPane(28);
        pane.toFront();
        pane.setLayoutY(getLayoutY());
        pane.setLayoutX(getLayoutX());

        final Image img = item.mipmap == null ? logo : item.mipmap;

        cover = setCoverIcon(img);
        cover.setEffect(shadow);

        pane.getChildren().add(cover);

        Label label = new Label();
        label.setLayoutX(cover.getLayoutX() + cover.getWidth() + 10);
        label.setLayoutY(2.5);

        loadDataAsync(item, label);

        label.setFont(ResourceManager.Instance.loadFont("main_font", 12));

        pane.getChildren().add(label);

        if (item.isPhantom()) {
            pane.setOpacity(0.75);
            pane.setEffect(new InnerShadow());
        }

        if(item.isNetty()) {
            setContextMenu(new SimilarContextMenu(item));
        } else {
            setContextMenu(new TrackContextMenu(item, this));
        }

        setPadding(new Insets(0));
        setGraphic(pane);
        setText(null);
    }

    private void loadDataAsync(Track item, Label label) {
        backgroundAlbumArtService.submit(() -> {
            Image img1 = !item.isNetty() ? item.getAlbumArt() : null;

            if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                Platform.runLater(() -> setBackgroundImageCentered(img1, getWidth(), background));
            }
        });

        dataLoadService.submit(() -> {
            String temp = getTopLabelText(item);

            Platform.runLater(() -> {
                if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                    label.setText(temp);
                }
            });

            Image buff = item.getMipmap(40);

            if(albumArtCounter.addAndGet(1) > Track.CACHE_SIZE) {
                Track.albumArtCleaner(getListView().getItems());

                albumArtCounter.set(0);
            }

            Color colorRes = ColorProcessor.core.getGeneralColorFromImage(buff);

            Platform.runLater(() -> {
                if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                    label.setTextFill(colorRes);

                    shadow.setColor(colorRes);

                    if(buff != null) {
                        cover.setFill(new ImagePattern(buff));
                    }
                }
            });

            if (item.isNetty() && item.equals(getItem()) && item.equals(current) && getItem().equals(current) && !isEmpty()) {
                item.setAlbumArt(item.getAlbumArt(size, size, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth));
            }
        });
    }

    private String getTopLabelText(Track item) {
        StringBuilder duration = new StringBuilder();

        if (saveFactoryTime) {
            return duration
                    .append(Track.getFormattedTotalDuration(Float.parseFloat(item.getLastTimeTrack())))
                    .append(" / ")
                    .append(Track.getFormattedTotalDuration(item.getTotalDurationInSeconds()))
                    .append(" ")
                    .append(item.viewName())
                    .append(" - ")
                    .append((getIndex() + 1))
                    .append(" - ")
                    .append(item.getExtension()).toString();
        }

        return item.viewName() + " - " + (getIndex() + 1) + " - " + item.getExtension();
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