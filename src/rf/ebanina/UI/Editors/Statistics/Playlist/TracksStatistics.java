package rf.ebanina.UI.Editors.Statistics.Playlist;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import rf.ebanina.File.Field;
import rf.ebanina.UI.Editors.Statistics.Track.TrackStatistics;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;

import java.util.ArrayList;
import java.util.List;

import static rf.ebanina.File.Field.getStat;
import static rf.ebanina.UI.Root.stage;

public class TracksStatistics
        extends Stage
{
    private TableView<TrackData> table;
    private final String[] allStatsColumns = {
            Field.DataTypes.COUNT_STREAM.code
    };

    private ObservableList<TrackData> trackData = FXCollections.observableArrayList();

    public TracksStatistics() {
        setTitle(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());

        table = new TableView<>();

        TableColumn<TrackData, String> trackCol = new TableColumn<>("Track");
        trackCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().track.viewName()));
        trackCol.setMinWidth(150);

        table.getColumns().add(trackCol);

        for (int i = 0; i < allStatsColumns.length; i++) {
            TableColumn<TrackData, String> col = createStatColumn(i);
            table.getColumns().add(col);
        }

        trackData.addAll(generateSampleTracks());
        table.setItems(trackData);

        BorderPane root = new BorderPane(table);
        root.setPadding(new Insets(10));
        Scene scene = new Scene(root, 600, 400);
        setScene(scene);

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TablePosition pos = table.getSelectionModel().getSelectedCells().get(0);
                int row = pos.getRow();
                TrackData rowData = table.getItems().get(row);

                TrackStatistics.instance.open(stage, rowData.track);
            }
        });

        initModality(Modality.APPLICATION_MODAL);
    }

    private TableColumn<TrackData, String> createStatColumn(int statIndex) {
        TableColumn<TrackData, String> col = new TableColumn<>(allStatsColumns[statIndex]);
        col.setMinWidth(100);
        col.setCellValueFactory(data -> {
            TrackData t = data.getValue();
            switch (statIndex) {
                case 0:
                    return new ReadOnlyStringWrapper(getStat(t.track, Field.fields.get(Field.DataTypes.COUNT_STREAM.code)));
                default:
                    return new ReadOnlyStringWrapper("");
            }
        });
        return col;
    }

    private List<TrackData> generateSampleTracks() {
        List<TrackData> tracks = new ArrayList<>();

        for(Track t : PlayProcessor.playProcessor.getTracks()) {
            tracks.add(new TrackData(t));
        }

        return tracks;
    }

    private static class TrackData {
        Track track;

        public TrackData(Track track) {
            this.track = track;
        }
    }
}
