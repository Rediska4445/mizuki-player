package rf.ebanina.UI.UI.Context.Menu.Playlist;

import com.jfoenix.controls.JFXCheckBox;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import rf.ebanina.UI.Editors.Metadata.Track.Metadata;
import rf.ebanina.UI.Editors.Statistics.Track.TrackStatistics;
import rf.ebanina.UI.Editors.Tags.Tags;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Context.Menu.ContextMenu;
import rf.ebanina.UI.UI.Context.Menu.ContextMenuItem;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellTrack;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static rf.ebanina.File.Resources.ResourceManager.getLocaleString;

public class TrackContextMenu
        extends ContextMenu
{
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Track track;

    private final ListCellTrack<?> sourceCell;

    private ContextMenuItem artistPageItem;

    private ContextMenuItem deleteFromPlaylistItem;
    private ContextMenuItem deleteTrackItem;
    private ContextMenuItem tagEditorItem;
    private ContextMenuItem openInExplorerItem;
    private ContextMenuItem isPhantom;
    private ContextMenuItem editTags;
    private ContextMenuItem trackItem;
    private javafx.scene.control.Menu copyToMenu;
    private javafx.scene.control.Menu moveToMenu;
    private ContextMenuItem openStatisticsItem;

    public TrackContextMenu(Track track, ListCellTrack<?> sourceCell) {
        this.track = track;
        this.sourceCell = sourceCell;

        initializeMenu();
    }

    private void initializeMenu() {
        trackItem = new ContextMenuItem();
        trackItem.setGraphic(new Label(track.viewName()));
        trackItem.setOnAction((e) -> {
            try {
                Root.rootImpl.openBrowser(new URI("https://www.google.com/search?q=" +
                        URLEncoder.encode(track.viewName(), StandardCharsets.UTF_8) +
                        "&gs_lcrp=EgZjaHJvbWUyBggAEEUYOTIICAEQABgNGB4yBwgCEAAY7wUyCggDEAAYgAQYogTSAQg3Mjg1ajBqN6gCALACAA&sourceid=chrome&ie=UTF-8"));
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        getItems().add(trackItem);
        getItems().add(new SeparatorMenuItem());

        artistPageItem = new ContextMenuItem();
        artistPageItem.setGraphic(new Label(getLocaleString("context_menu_show_tracks_by_artist", "Show all by author") + ": " + track.artist));
        artistPageItem.setOnAction(e -> showTracksByArtist());

        openStatisticsItem = new ContextMenuItem();
        openStatisticsItem.setGraphic(new Label(getLocaleString("context_menu_show_track_statistics", "Stats") + ": " + track.viewName));
        openStatisticsItem.setOnAction(e -> openStatistics());

        deleteFromPlaylistItem = new ContextMenuItem();
        deleteFromPlaylistItem.setGraphic(new Label(getLocaleString("context_menu_delete_track_from_playlist", "Delete track from playlist")));
        deleteFromPlaylistItem.setOnAction(e -> deleteFromPlaylist());

        deleteTrackItem = new ContextMenuItem();
        deleteTrackItem.setGraphic(new Label(getLocaleString("context_menu_delete_track", "Open In Explorer")));
        deleteTrackItem.setOnAction(e -> deleteTrack());

        tagEditorItem = new ContextMenuItem();
        tagEditorItem.setGraphic(new Label(track.viewName() + " - " + getLocaleString("context_menu_manage_tags", "Tags manage")));
        tagEditorItem.setOnAction(e -> openTagEditor());

        isPhantom = new ContextMenuItem();

        JFXCheckBox checkBox = new JFXCheckBox();
        checkBox.setSelected(track.isPhantom());

        HBox phantom = new HBox(new Label(getLocaleString("context_menu_phantom", "It's Phantom") + " - " + track.viewName), checkBox);
        phantom.setSpacing(10);

        Root.rootImpl.initPantyhose(checkBox);

        isPhantom.setGraphic(phantom);
        isPhantom.setOnAction(e -> {
            track.setPhantom(checkBox.isSelected());
            checkBox.setSelected(!track.isPhantom());
        });

        editTags = new ContextMenuItem();
        editTags.setGraphic(new Label(getLocaleString("context_menu_edit_item", "Edit Item") + " - " + track.viewName));
        editTags.setOnAction(e -> {
            Metadata.getInstance().setTrack(track);
            Metadata.getInstance().open(Root.rootImpl.stage);
        });

        copyToMenu = new javafx.scene.control.Menu();
        copyToMenu.setGraphic(new Label(getLocaleString("context_menu_copy_to", "Copy to")));

        moveToMenu = new javafx.scene.control.Menu();
        moveToMenu.setGraphic(new Label(getLocaleString("context_menu_move_to", "Move to")));

        openInExplorerItem = new ContextMenuItem();
        openInExplorerItem.setGraphic(new Label(getLocaleString("context_menu_open_in_explorer", "Open In Explorer")));
        openInExplorerItem.setOnAction(e -> openTrackInExplorer());

        for (Playlist folder : PlayProcessor.playProcessor.getCurrentPlaylist()) {
            Label playlist = new Label(folder.getPath());
            playlist.setTextFill(Color.WHITE);

            ContextMenuItem copyItem = new ContextMenuItem(playlist);
            copyItem.setOnAction(e -> copyToFolder(folder.getPath(), track));

            copyToMenu.getItems().add(copyItem);

            ContextMenuItem moveItem = new ContextMenuItem(playlist);
            moveItem.setOnAction(e -> moveToFolder(folder.getPath()));

            moveToMenu.getItems().add(moveItem);
        }

        ContextMenuItem copyNewItem = new ContextMenuItem(createNewPlaylist(actionEvent -> {
            final Path of = Path.of(PlayProcessor.playProcessor.currentDefaultMusicDir + File.separator + actionEvent);

            if(!Files.exists(of)) {
                try {
                    Files.createDirectory(of);

                    copyToFolder(of.toString(), track);

                    PlayProcessor.playProcessor.getCurrentPlaylist().add(new Playlist(of.toString()));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                copyToFolder(of.toString(), track);
            }
        }));

        ContextMenuItem moveNewItem = new ContextMenuItem(createNewPlaylist(actionEvent -> {
            final Path of = Path.of(PlayProcessor.playProcessor.currentDefaultMusicDir + File.separator + actionEvent);

            if(!Files.exists(of)) {
                try {
                    Files.createDirectory(of);

                    moveToFolder(of.toString());

                    PlayProcessor.playProcessor.getCurrentPlaylist().add(new Playlist(of.toString()));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                moveToFolder(of.toString());
            }
        }));

        moveToMenu.getItems().add(moveNewItem);
        copyToMenu.getItems().add(copyNewItem);

        this.getItems().addAll(
                artistPageItem,
                copyToMenu,
                moveToMenu,
                deleteFromPlaylistItem,
                deleteTrackItem,
                openInExplorerItem,
                openStatisticsItem,
                isPhantom,
                editTags,
                tagEditorItem
        );
    }

    private void openStatistics() {
        TrackStatistics.instance.open(Root.rootImpl.stage, this.track);
    }

    protected static Parent createNewPlaylist(Consumer<String> event) {
        HBox newPlaylist = new HBox();
        TextField name = new TextField();
        name.setPromptText("<" + getLocaleString("context_menu_new_playlist", "new playlist") + ">");

        Button create = new Button("copy");
        create.setOnAction((e) -> event.accept(name.getText()));
        Root.rootImpl.initPantyhose(create, name);

        newPlaylist.getChildren().addAll(name, create);

        return newPlaylist;
    }

    private void showTracksByArtist() {
        Root.rootImpl.tracksListView.getCurrentPlaylistText().setText(track.getArtist());
        Root.rootImpl.tracksListView.getTrackListView().getItems().clear();

        executorService.submit(() -> {
            try (Stream<Path> stream = Files.walk(Paths.get(PlayProcessor.playProcessor.getCurrentDefaultMusicDir()), FOLLOW_LINKS)) {
                stream.map(Path::toFile)
                        .filter(file -> file.getName().contains(track.artist))
                        .map(File::getAbsolutePath)
                        .map(Track::new)
                        .forEach(trackItem -> Platform.runLater(() -> Root.rootImpl.tracksListView.getTrackListView().getItems().add(trackItem)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    protected static void copyToFolder(String folder, Track track) {
        try {
            Path source = Path.of(track.toString());
            Path dest = Path.of(folder, source.getFileName().toString());
            Files.copy(source, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void moveToFolder(String folder) {
        try {
            Path source = Path.of(track.toString());
            Path dest = Path.of(folder, source.getFileName().toString());
            Files.move(source, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteFromPlaylist() {
        if (track.toString().equals(PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).toString()))
            PlayProcessor.playProcessor.next();

        Platform.runLater(() -> {
            Root.rootImpl.tracksListView.getTrackListView().getItems().remove(track);
            PlayProcessor.playProcessor.getTracks().remove(track);
        });
    }

    private void deleteTrack() {
        deleteFromPlaylist();

        Desktop.getDesktop().moveToTrash(new File(track.toString()));
    }

    private void openTrackInExplorer() {
        try {
            Root.rootImpl.openInExplorer(track.getPath()).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void openTagEditor() {
        Tags.getInstance().setTrack(track);
        Tags.getInstance().open(Root.rootImpl.stage);

        if (sourceCell != null) {
            Platform.runLater(() -> {
                ListView<Track> listView = sourceCell.getListView();

                if (listView != null) {
                    int index = sourceCell.getIndex();

                    if (index >= 0 && index < listView.getItems().size()) {
                        listView.getItems().set(index, track);
                    }
                }
            });
        }
    }
}