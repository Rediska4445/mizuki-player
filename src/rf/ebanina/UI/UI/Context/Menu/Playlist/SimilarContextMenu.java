package rf.ebanina.UI.UI.Context.Menu.Playlist;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Context.Menu.ContextMenu;
import rf.ebanina.UI.UI.Context.Menu.ContextMenuItem;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.utils.concurrency.LonelyThreadPool;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import static rf.ebanina.File.Localization.LocalizationManager.getLocaleString;
import static rf.ebanina.UI.Root.similar;
import static rf.ebanina.UI.UI.Context.Menu.Playlist.TrackContextMenu.copyToFolder;
import static rf.ebanina.UI.UI.Context.Menu.Playlist.TrackContextMenu.createNewPlaylist;

public class SimilarContextMenu
        extends ContextMenu
{
    private final LonelyThreadPool exec = new LonelyThreadPool();
    private ContextMenuItem trackItem;
    private final Track item;

    private void deleteFromPlaylist() {
        if(item.getPath() == null) {
            Platform.runLater(() -> similar.getTrackListView().getItems().remove(similar.getTrackListView().getSelectionModel().getSelectedIndex()));

            return;
        }

        Platform.runLater(() -> {
            similar.getTrackListView().getItems().remove(item);
        });
    }

    public SimilarContextMenu(Track item) {
        super();

        this.item = item;

        trackItem = new ContextMenuItem();
        trackItem.setGraphic(new Label(item.viewName()));
        trackItem.setOnAction((e) -> {
            try {
                Root.rootImpl.openBrowser(new URI("https://www.google.com/search?q=" +
                        URLEncoder.encode(item.viewName(), StandardCharsets.UTF_8) +
                        "&gs_lcrp=EgZjaHJvbWUyBggAEEUYOTIICAEQABgNGB4yBwgCEAAY7wUyCggDEAAYgAQYogTSAQg3Mjg1ajBqN6gCALACAA&sourceid=chrome&ie=UTF-8"));
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        MenuItem removeButton = new MenuItem();
        removeButton.setGraphic(new Label(getLocaleString("context_menu_remove", "Remove")));
        removeButton.setOnAction((e) -> {
            deleteFromPlaylist();
        });

        getItems().add(removeButton);

        MenuButton menuButtonCopy = new MenuButton();
        Root.rootImpl.initPantyhose(menuButtonCopy);

        menuButtonCopy.setGraphic(new Label(getLocaleString("context_menu_download_to", "Download to")));
        menuButtonCopy.setPopupSide(Side.RIGHT);

        Iterator<Playlist> iter = PlayProcessor.playProcessor.getCurrentPlaylist().iterator();

        while (iter.hasNext()) {
            Playlist cr = iter.next();

            MenuItem item1 = new MenuItem();
            item1.setOnAction(e11 -> exec.runNewTask(() -> Track.downloadAndSaveDataAttributeToDirectory(item, Paths.get(cr.getPath()))));
            item1.setGraphic(new rf.ebanina.UI.UI.Element.Text.Label(cr.getPath()));

            menuButtonCopy.getItems().add(item1);
        }

        ContextMenuItem copyNewItem = new ContextMenuItem(createNewPlaylist(actionEvent -> {
            exec.runNewTask(() -> {
                final Path of = Path.of(PlayProcessor.playProcessor.getCurrentDefaultMusicDir(), actionEvent);

                if (!Files.exists(of)) {
                    try {
                        Files.createDirectory(of);

                        copyToFolder(of.toString(), item);

                        PlayProcessor.playProcessor.getCurrentPlaylist().add(new Playlist(of.toString()));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    copyToFolder(of.toString(), item);
                }
            });
        }));

        menuButtonCopy.getItems().add(copyNewItem);

        ContextMenuItem getPossibleUrlItem = new ContextMenuItem();
        getPossibleUrlItem.setGraphic(new rf.ebanina.UI.UI.Element.Text.Label(getLocaleString("context_menu_get_possible_url", "Get possible url")));
        getPossibleUrlItem.setOnAction(e11 -> {
            try {
                exec.runNewTask(() -> {
                    try {
                        URL url = Track.getURIFromTrack(item);

                        if (url != null) {
                            URI uri = url.toURI();

                            StringSelection selection = new StringSelection(uri.toString());
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        ContextMenuItem openPossibleUrlItem = new ContextMenuItem();
        openPossibleUrlItem.setGraphic(new rf.ebanina.UI.UI.Element.Text.Label(getLocaleString("context_menu_open_possible_url", "Open possible url")));
        openPossibleUrlItem.setOnAction(e11 -> exec.runNewTask(() -> {
            try {
                URL url = Track.getURIFromTrack(item);

                if (url != null) {
                    URI uri = url.toURI();

                    Desktop.getDesktop().browse(uri);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        getItems().addAll(openPossibleUrlItem, getPossibleUrlItem);

        MenuItem copyTo = new MenuItem();
        copyTo.setGraphic(menuButtonCopy);

        getItems().addAll(copyTo);
    }
}