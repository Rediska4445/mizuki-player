package rf.ebanina.File;

import rf.ebanina.File.Resources.Resources;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static rf.ebanina.File.Resources.ResourceManager.getLocaleString;

//TODO: Убрать ООП архитектуру. Сделать простыми строками
//TODO: Убрать этот класс вообще 17.11.25
public class Field implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    public static final Map<String, Field> fields = new HashMap<>(Map.ofEntries(
            Map.entry(DataTypes.TIME.code,
                    new Field(
                            "time", getLocaleString("statistics_last_time", "last time"), "0"
                    )),
            //<----------------------------------------------------------------->
            Map.entry(DataTypes.VOLUME.code,
                    new Field(
                            "volume", getLocaleString("statistics_volume", "volume"), "0"
                    )),
            //<----------------------------------------------------------------->
            Map.entry(DataTypes.COUNT_STREAM.code,
                    new Field(
                            "count streams", getLocaleString("statistics_count_of_streams", "count streams"), "0"
                    )),
            //<----------------------------------------------------------------->
            Map.entry(DataTypes.AVERAGE_PLAY.code,
                    new Field(
                            "average play time", getLocaleString("statistics_average_play_time", "average play time"), "0"
                    )),
            //<----------------------------------------------------------------->
            Map.entry(DataTypes.AVERAGE_TEMPO.code,
                    new Field(
                            "average tempo", getLocaleString("statistics_average_tempo", "average tempo"), "0"
                    )),
            //<----------------------------------------------------------------->
            Map.entry(DataTypes.PITCH.code,
                    new Field(
                            "last pitch", getLocaleString("statistics_last_listen_pitch", "last pitch"), "1"
                    )),
            //<----------------------------------------------------------------->
            Map.entry(DataTypes.COUNT_SELECTED_FROM_PLAYLIST.code,
                    new Field(
                            "count selected from playlist", getLocaleString("statistics_count_selected_from_playlist", "count selected from playlist"), "0"
                    )),
            Map.entry(DataTypes.COUNT_PLAY.code,
                    new Field(
                            "count play", getLocaleString("statistics_volume", "count play"), "0"
                    )),
            Map.entry(DataTypes.COUNT_FULLY_PLAY.code,
                    new Field(
                            "count full play", getLocaleString("statistics_full_play", "full play"), "0"
                    )),
            Map.entry(DataTypes.LIKE_MOMENT_START.code,
                    new Field(
                            "like moment start", getLocaleString("statistics_like_moment_start", "like moment start"), "0"
                    )),
            Map.entry(DataTypes.LIKE_MOMENT_STOP.code,
                    new Field(
                            "like moment stop", getLocaleString("statistics_like_moment_stop", "like moment stop"), "0"
                    )),
            Map.entry(DataTypes.PLAYLIST_LAST_INDEX.code,
                    new Field(
                            "last index", getLocaleString("statistics_playlist_last_index", "last index"), "0"
                    )),
            Map.entry(DataTypes.TEMPO.code,
                    new Field(
                            "tempo", getLocaleString("statistics_playlist_last_tempo", "last tempo"), "1"
                    )),
            Map.entry(DataTypes.PAN.code,
                    new Field(
                            "pan", getLocaleString("statistics_playlist_last_pan", "last pan"), "0"
                    ))
    ));

    public static String getStat(Track track, Field field) {
        return FileManager.instance.read(
                Path.of(Resources.Properties.DEFAULT_CACHE_TRACKS_PATH.getKey()
                        + File.separator
                        + FileManager.instance.name(new Playlist(Path.of(track.getPath()).getParent().toString()).getFileName())).toAbsolutePath().toString(),
                track.toString(),
                field.getLocalName(),
                field.getResultIfNull()
        );
    }

    private String localName;
    private String eternalName;
    private String resultIfNull;

    public Field(String local_name, String eternal_name, String result_if_null) {
        this.localName = local_name;
        this.eternalName = eternal_name;
        this.resultIfNull = result_if_null;
    }

    public String getResultIfNull() {
        return resultIfNull;
    }

    public String getLocalName() {
        return localName;
    }

    public String getEternalName() {
        return eternalName;
    }

}