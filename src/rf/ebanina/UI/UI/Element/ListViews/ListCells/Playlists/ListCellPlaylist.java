package rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Metadata.MetadataOfFile;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Context.Menu.Playlist.PlaylistContextMenu;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.AnimatedListCell;
import rf.ebanina.UI.UI.Element.Text.Label;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class ListCellPlaylist<T>
        extends AnimatedListCell<Playlist>
{
    private final Image defaultLogo;
    private static final ExecutorService exec = Executors.newFixedThreadPool(2);

    protected Label title;

    private java.util.concurrent.Future<?> currentTask;

    protected HBox root;

    public ListCellPlaylist(Image logo) {
        this(ColorProcessor.core.getMainClr(), logo);
    }

    public ListCellPlaylist(Color color, Image logo) {
        super();

        this.defaultLogo = logo;

        root = new HBox();
        root.setSpacing(6);
        root.setPadding(new Insets(0));

        createBackgroundPane(28);

        initCoverIcon();

        title = new Label();

        root.getChildren().add(cover);
        root.getChildren().add(title);

        pane.getChildren().add(root);

        setPadding(new Insets(0));
    }

    @Override
    public void updateItem(Playlist item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            initArt(item);

            setContextMenu(new PlaylistContextMenu(item, this));
            setGraphic(pane);
        }
    }

    private File getFirstFile(Playlist item) {
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

        return firstFile;
    }

    private void initArt(Playlist item) {
        currentTask = exec.submit(() -> {
            File firstFile = getFirstFile(item);

            final Image mipmap;
            final Image art;

            if (firstFile != null) {
                Image temp = MetadataOfFile.metadataOfFilesImpl.getArt(new Track(firstFile.getAbsolutePath()), Track.mipmapSize, Track.mipmapSize, isPreserveRatio, isSmooth);
                mipmap = temp == null ? defaultLogo : temp;

                Image temp1 = MetadataOfFile.metadataOfFilesImpl.getArt(new Track(firstFile.getAbsolutePath()), Track.albumArtSize, Track.albumArtSize, isPreserveRatio, isSmooth);
                art = temp1 == null ? defaultLogo : temp1;
            } else {
                art = defaultLogo;
                mipmap = defaultLogo;
            }

            Platform.runLater(() -> {
                cover.setFill(new ImagePattern(mipmap));
                cover.setEffect(shadow);
                title.setText(item.getName());

                super.setBackgroundImageCentered(art, getWidth(), background);
            });
        });
    }

    @Override
    protected Node createExtraInfoContent() {
        return null;
    }

    @Override
    public void onItemDropped(int draggedIndex, int targetIndex) {
        Playlist temp = getListView().getItems().get(draggedIndex);
        getListView().getItems().set(draggedIndex, getListView().getItems().get(targetIndex));
        getListView().getItems().set(targetIndex, temp);

        Playlist temp1 = getListView().getItems().get(draggedIndex);
        PlayProcessor.playProcessor.getCurrentPlaylist().set(draggedIndex, PlayProcessor.playProcessor.getCurrentPlaylist().get(targetIndex));
        PlayProcessor.playProcessor.getCurrentPlaylist().set(targetIndex, temp1);

        Root.rootImpl.tracksListView.getPlaylistListView().getSelectionModel().select(PlayProcessor.playProcessor.getCurrentPlaylistIter());
    }
}
