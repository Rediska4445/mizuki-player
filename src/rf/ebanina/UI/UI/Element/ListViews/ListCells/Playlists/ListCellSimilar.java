package rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Context.Menu.Playlist.SimilarContextMenu;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Element.Text.Label;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.*;

//TODO: Merge with ListCellTrack
@Deprecated
public class ListCellSimilar extends AnimatedListCell<Track> {
    private static final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final ExecutorService backgroundAlbumArtService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private volatile Track current = null;

    private final Label label = new Label();

    public ListCellSimilar() {
        super(ColorProcessor.core.getMainClr());

        setAlignment(Pos.CENTER);
        setPadding(new Insets(0));
    }

    @Override
    protected void updateItem(Track item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        current = item;

        pane = createBackgroundPane(28);
        pane.toFront();
        pane.setLayoutY(getLayoutY());
        pane.setLayoutX(getLayoutX());

        cover = setCoverIcon(logo);
        cover.setWidth(18);
        cover.setHeight(18);
        cover.setLayoutX(5);
        cover.setLayoutY(2.5);
        cover.setEffect(shadow);

        label.setFont(ResourceManager.Instance.loadFont("main_font", 12));
        label.setLayoutX(cover.getLayoutX() + cover.getWidth() + 6);
        label.setLayoutY(2.5);

        pane.getChildren().addAll(cover, label);

        setContextMenu(new SimilarContextMenu(item));
        updateAsync(item);

        setGraphic(pane);
    }

    private void updateAsync(Track item) {
        backgroundAlbumArtService.submit(() -> {
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
                    if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                        Platform.runLater(() -> setBackgroundImageCentered(buff, getWidth(), background));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        exec.submit(() -> {
            try {
                Image buff;

                String url = item.metadata.get("album_art", String.class);

                if(url != null) {
                    buff = item.mipmap == null || !item.metadata.get("mipmap_is_loaded", boolean.class) ? item
                            .setMipmap(new Image(item.metadata.get("album_art", String.class), 40, 40, isPreserveRatio, isSmooth)).getMipmap()
                            : item.getMipmap();

                    item.metadata.put("mipmap_is_loaded", true, boolean.class);
                } else {
                    buff = item.getMipmap();
                }

                Color color = ColorProcessor.core.getGeneralColorFromImage(buff);

                String viewName = item.viewName();

                Platform.runLater(() -> {
                    if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                        cover.setFill(new ImagePattern(buff != null ? buff : logo));
                        shadow.setColor(color);
                        label.setTextFill(color);
                        label.setText(viewName + " / " + item);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onItemDropped(int draggedIndex, int targetIndex) {
        Track temp = Root.similar.getTrackListView().getItems().get(draggedIndex);
        Root.similar.getTrackListView().getItems().set(draggedIndex, getListView().getItems().get(targetIndex));
        Root.similar.getTrackListView().getItems().set(targetIndex, temp);

        if (PlayProcessor.playProcessor.isNetwork()) {
            Track temp1 = PlayProcessor.playProcessor.getTracks().get(draggedIndex);
            PlayProcessor.playProcessor.getTracks().set(draggedIndex, PlayProcessor.playProcessor.getTracks().get(targetIndex));
            PlayProcessor.playProcessor.getTracks().set(targetIndex, temp1);
        }

        Root.similar.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());
    }
}

