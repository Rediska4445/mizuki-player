package rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Metadata.MetadataOfFile;
import rf.ebanina.UI.UI.Context.Menu.Playlist.PlaylistContextMenu;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Element.Text.Label;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static rf.ebanina.UI.Root.tracksListView;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.*;

public class ListCellPlaylist<T> extends AnimatedListCell<Playlist> {
    private final Image defaultLogo;

    public ListCellPlaylist(Image logo) {
        this(ColorProcessor.core.getMainClr(), logo);
    }

    public ListCellPlaylist(Color color, Image logo) {
        super(color);

        this.defaultLogo = logo;
    }

    private static final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private volatile Playlist current;

    @Override
    public void updateItem(Playlist item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            pane = createBackgroundPane();

            current = item;

            HBox root = new HBox();
            root.setSpacing(6);
            root.setPadding(new Insets(5, 0, 5, 3.5));

            pane.getChildren().add(root);

            initArt(root, item);

            setContextMenu(new PlaylistContextMenu(item, this));
            setPrefHeight(26);
            setGraphic(pane);
        }
    }

    private void initArt(Pane root, Playlist item) {
        exec.submit(() -> {
            File firstFile = null;

            try (Stream<Path> pathStream = Files.walk(Paths.get(item.getPath()))) {
                Optional<Path> firstPath = pathStream
                        .filter(Files::isRegularFile)
                        .filter(t -> FileManager.instance.hasSupportedExtension(t))
                        .findFirst();

                if (firstPath.isPresent()) {
                    firstFile = firstPath.get().toFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            final Image mipmap;

            final Image art;

            if (firstFile != null) {
                Image temp = MetadataOfFile.iMetadataOfFiles.getArt(new Track(firstFile.getAbsolutePath()), 40, 40, isPreserveRatio, isSmooth);

                mipmap = temp == null ? defaultLogo : temp;

                Image temp1 = MetadataOfFile.iMetadataOfFiles.getArt(new Track(firstFile.getAbsolutePath()), size, size, isPreserveRatio, isSmooth);

                art = temp1 == null ? defaultLogo : temp1;
            } else {
                art = null;

                mipmap = defaultLogo;
            }

            Platform.runLater(() -> {
                cover = setCoverIcon(mipmap);
                cover.setEffect(shadow);

                final Label label = new Label(item.getName(), Color.BLACK);

                root.getChildren().add(cover);
                root.getChildren().add(label);
            });

            if (item.equals(getItem()) && item.equals(current) && getItem().equals(current)) {
                Platform.runLater(() -> setBackgroundImageCentered(art, getWidth(), background));
            }
        });
    }

    @Override
    public void onItemDropped(int draggedIndex, int targetIndex) {
        Playlist temp = tracksListView.getPlaylistListView().getItems().get(draggedIndex);
        tracksListView.getPlaylistListView().getItems().set(draggedIndex, getListView().getItems().get(targetIndex));
        tracksListView.getPlaylistListView().getItems().set(targetIndex, temp);

        Playlist temp1 = PlayProcessor.playProcessor.getCurrentPlaylist().get(draggedIndex);
        PlayProcessor.playProcessor.getCurrentPlaylist().set(draggedIndex, PlayProcessor.playProcessor.getCurrentPlaylist().get(targetIndex));
        PlayProcessor.playProcessor.getCurrentPlaylist().set(targetIndex, temp1);

        tracksListView.getPlaylistListView().getSelectionModel().select(PlayProcessor.playProcessor.getCurrentPlaylistIter());
    }
}
