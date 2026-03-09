package ebanina.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellPlaylist;
import rf.ebanina.UI.UI.Element.ListViews.ListCells.Playlists.ListCellTrack;
import rf.ebanina.UI.UI.Element.ListViews.Playlist.PlayView;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PlayViewTestApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        PlayView<Track, Playlist> playView = new PlayView<>();

        PlayProcessor<Track, Playlist> playProcessor = new PlayProcessor<>("");
        playProcessor.setTracks(new ArrayList<>(List.of(
                new Track("Test Track 1").setPlaylist("play1"),
                new Track("Test Track 2").setPlaylist("play1").setArtist("Bla"),
                new Track("Test Track 3").setPlaylist("play1").setTitle("Asda"),
                new Track("Test Track 4").setPlaylist("play1").setPath("esdfweer"),
                new Track("Test Track 5").setPlaylist("play1").setTotalDuraSec(50),
                new Track("Test Track 6").setPlaylist("play2").setArtist("Artist1").setTitle("Title1"),
                new Track("Test Track 7").setPlaylist("play2").setArtist("Artist2").setTitle("Title2"),
                new Track("Test Track 8").setPlaylist("play2").setArtist("Artist3").setTitle("Title3"),
                new Track("Test Track 9").setPlaylist("play3").setArtist("Artist4").setTitle("Title4").setTotalDuraSec(120),
                new Track("Test Track 10").setPlaylist("play3").setArtist("Artist5").setTitle("Title5").setPath("/music/track10.mp3"),
                new Track("Test Track 11").setPlaylist("play4").setArtist("Artist6"),
                new Track("Test Track 12").setPlaylist("play4").setArtist("Artist7"),
                new Track("Test Track 13").setPlaylist("play4").setArtist("Artist8").setTags(new ArrayList<>(List.of(
                        new Track.Tag("shit-0")
                ))),
                new Track("Test Track 14").setPlaylist("play5").setTitle("Alpha").setTags(new ArrayList<>(List.of(
                        new Track.Tag("shit-1")
                ))),
                new Track("Test Track 15").setPlaylist("play5").setTitle("Beta").setTags(new ArrayList<>(List.of(
                        new Track.Tag("shit-1"),
                        new Track.Tag("shit-0")
                ))),
                new Track("Test Track 16").setPlaylist("play5").setPath("/audio/track16.wav"),
                new Track("Test Track 17").setPlaylist("play6").setPath("/audio/track17.wav").setTotalDuraSec(90),
                new Track("Test Track 18").setPlaylist("play6").setArtist("Artist9").setTitle("Gamma"),
                new Track("Test Track 19").setPlaylist("play7").setArtist("Artist10").setTitle("Delta"),
                new Track("Test Track 20").setPlaylist("play7").setArtist("Artist11").setTitle("Epsilon"),
                new Track("Test Track 21").setPlaylist("play8").setTitle("Zeta").setPath("/music/track21.mp3"),
                new Track("Test Track 22").setPlaylist("play8").setArtist("Artist12").setTotalDuraSec(150),
                new Track("Test Track 23").setPlaylist("play9").setTitle("Eta"),
                new Track("Test Track 24").setPlaylist("play9").setArtist("Artist13"),
                new Track("Test Track 25").setPlaylist("play10").setArtist("Artist14").setTitle("Theta").setTotalDuraSec(75)
        )));

        playView.setPlayProcessor(playProcessor);

        playProcessor.setCurrentPlaylist(new ArrayList<>(List.of(
                new Playlist("Test Playlist 1", "Playlist 1"),
                new Playlist("Test Playlist 2", "Playlist 2"),
                new Playlist("Test Playlist 3", "Playlist 3"),
                new Playlist("Test Playlist 4", "Playlist 4"),
                new Playlist("Test Playlist 5", "Playlist 5")
        )));

        playView.getPlaylistListView().getItems().addAll(playProcessor.getCurrentPlaylist());

        playView.getTrackListView().getItems().addAll(playProcessor.getTracks());

        playView.getTrackListView().setCellFactory(e -> new ListCellTrack<>(false,
                new Image(Paths.get(new File("C:\\Users\\2022\\Desktop\\программы\\Ebanina-Test\\Ebanina\\Ebanina-VST\\res\\visual\\gui\\logo.jpg")
                        .getAbsolutePath()).toUri().toString())));

        playView.getPlaylistListView().setCellFactory(e -> new ListCellPlaylist<>(
                new Image(Paths.get(new File("C:\\Users\\2022\\Desktop\\программы\\Ebanina-Test\\Ebanina\\Ebanina-VST\\res\\visual\\gui\\logo.jpg")
                        .getAbsolutePath()).toUri().toString())));

        playView.getBtnPlaylist().setOnAction((e) -> {
            if(playView.getTrackListView().isVisible()) {
                playView.closeTrackList();
            } else {
                playView.openTrackList();
            }
        });

        Scene scene = new Scene(playView, 400, 300);
        primaryStage.setTitle("PlayView Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
