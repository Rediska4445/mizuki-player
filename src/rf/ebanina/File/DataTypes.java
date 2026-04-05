package rf.ebanina.File;

public enum DataTypes {
    TIME("time"),
    VOLUME("volume"),
    COUNT_STREAM("count streams"),
    AVERAGE_PLAY("average play time"),
    AVERAGE_TEMPO("average tempo"),
    LAST_DATE("last date"),
    PITCH("pitch"),
    COUNT_SELECTED_FROM_PLAYLIST("count selected from playlist"),
    COUNT_PLAY("count play"),
    COUNT_FULLY_PLAY("count full play"),
    LIKE_MOMENT_START("like moment start"),
    LIKE_MOMENT_STOP("like moment stop"),
    PLAYLIST_LAST_INDEX("last index"),
    TOTAL_TIME_PLAYED("total time played"),
    TEMPO("tempo"),
    PAN("pan");

    public final String code;

    DataTypes(String code) {
        this.code = code;
    }
}
