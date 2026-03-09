package rf.ebanina.File.Localization;

public enum Locales {
    TOOLTIP_MAIN_PLAY("tooltip_main_play", "play"),
    TOOLTIP_MAIN_NEXT("tooltip_main_next", "next"),
    TOOLTIP_MAIN_PREV("tooltip_main_prev", "prev"),
    TOOLTIP_MAIN_FUNCTIONS_BUTTON("tooltip_all_functions_button", "main"),
    TOOLTIP_MAIN_SLIDER("tooltip_main_slider", "slider"),
    TOOLTIP_PLAYLIST_SEARCH("tooltip_playlist_search", "search"),

    TOOLTIP_PLAYLIST_PREV("tooltip_playlist_prev_playlist", "playlist prev"),
    TOOLTIP_PLAYLIST_SET("tooltip_playlist_set_playlist", "playlist"),
    TOOLTIP_PLAYLIST_NEXT("tooltip_playlist_next_playlist", "playlist next"),

    TOOLTIP_OPEN_LOCAL_PLAYLIST("tooltip_main_hide_right", "playlist next"),
    TOOLTIP_OPEN_NETWORK_PLAYLIST("tooltip_main_hide_left", "playlist next"),

    SKIP_INTRO("skip_audio_intro", "skip intro"),
    SKIP_PIT("skip_audio_pit", "skip pit");

    public final String code;
    public final String defaultValue;

    Locales(String code, String defaultValue) {
        this.code = code;
        this.defaultValue = defaultValue;
    }
}
