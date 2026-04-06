package rf.ebanina.UI.UI.Context.Menu.Playlist;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import rf.ebanina.File.Localization.LocalizationManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Context.Menu.ContextMenu;
import rf.ebanina.UI.UI.Context.Menu.ContextMenuItem;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellPlaylist;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Playlist;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;

public class PlaylistContextMenu extends ContextMenu {
    private ContextMenuItem deleteFromDisk;
    private ContextMenuItem removeFromList;
    private ContextMenuItem playlistItem;
    private ContextMenuItem renameItem;
    private ContextMenuItem editorItem;
    private ContextMenuItem aboutPlaylist;

    private ListCellPlaylist<?> sourceCell;
    private Playlist track;

    public PlaylistContextMenu(Playlist track, ListCellPlaylist<?> sourceCell) {
        this.sourceCell = sourceCell;
        this.track = track;

        init();
    }

    private void init() {
        playlistItem = new ContextMenuItem();
        playlistItem.setGraphic(new Label(track.getName()));
        playlistItem.setOnAction((e) -> {
            try {
                Root.rootImpl.openInExplorer(track.getPath()).start();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        });

        editorItem = new ContextMenuItem();
        editorItem.setGraphic(new Label(LocalizationManager.getLocaleString("context_menu_playlist_editor", "Playlist editor")));

        renameItem = new ContextMenuItem();

        TextField renameTo = new TextField();
        renameTo.setPromptText(LocalizationManager.getLocaleString("context_menu_rename", "Rename to"));

        Button renameToAccept = new Button("OK");

        renameItem.setGraphic(new HBox(renameTo, renameToAccept));
        renameToAccept.setOnAction((e) -> {
            File playlist = new File(track.getPath());

            sourceCell.getListView().getItems().set(
                    sourceCell.getListView().getItems().indexOf(track),
                    new Playlist(playlist.getAbsolutePath())
            );
        });

        deleteFromDisk = new ContextMenuItem();
        deleteFromDisk.setGraphic(new Label(getLocaleString("context_menu_remove_playlist_from_disk", "Remove from disk")
                + ": " + track.getName()));
        deleteFromDisk.setOnAction(e -> {
            try {
                deletePlaylist(Path.of(track.getPath()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        removeFromList = new ContextMenuItem();
        removeFromList.setGraphic(new Label(getLocaleString("context_menu_remove_playlist_from_list", "Remove from list")
                + ": " + track.getName()));
        removeFromList.setOnAction(e -> {
            removeFromListInternal();
        });

        aboutPlaylist = new ContextMenuItem(getLocaleString("context_menu_about_playlist", "About playlist")
                + ": " + track.getName());
        aboutPlaylist.setOnAction(e -> {
            //TODO: About playlist editor page
        });

        this.getItems().addAll(
                playlistItem,
                new SeparatorMenuItem(),
                renameItem,
                removeFromList,
                deleteFromDisk
        );
    }

    private void deletePlaylist(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Desktop.getDesktop().moveToTrash(new File(sourceCell.getItem().getPath()));

        PlayProcessor.playProcessor.getCurrentPlaylist().remove(sourceCell.getItem());
        Root.rootImpl.tracksListView.getPlaylistListView().getItems().remove(sourceCell.getItem());
    }

    private void removeFromListInternal() {
        Platform.runLater(() -> {
            PlayProcessor.playProcessor.getCurrentPlaylist().remove(sourceCell.getItem());
            sourceCell.getListView().getItems().remove(sourceCell.getItem());
        });
    }
}