package rf.ebanina.File.Resources;

import java.io.File;

public final class Resources {
    public static class ID {
        public static final String AUDIO_INTRO_FILE = "audio_intro";
    }

    public static class Types {
        public static final String STYLESHEET = "stylesheet";
        public static final String IMAGE = "image";
        public static final String FXML = "fxml";
        public static final String FONT = "font";
        public static final String FILE = "file";
        public static final String SVG = "svg";
    }

    public enum Properties {
        RESOURCES("res"),

        MODS("modsPath"),

        FXML_DOWNLOAD_INET_PATH("FXMLDownloadInetPath"),
        FXML_AUDIO_HOST_PATH("FXMLAudioHostPath"),
        FXML_TAG_EDITOR_PATH("FXMLTagEditorPath"),
        FXML_SETTINGS_PATH("FXMLSettingsPath"),
        FXML_META_DATA_PATH("FXMLMetaDataPath"),

        CONTEXT_MENU_STYLES("contextMenuStyles"),
        LISTVIEW("listview"),
        SCROLLBAR_FIXED_WIDTH("scrollbar-fixed-width"),
        SLIDER_CUSTOM("slider-custom"),
        ROOT("root"),
        VSTHOST("vsthost"),
        TABPANE("tabpane"),

        ALBUM_ART_LOGO("album_art_logo"),

        PLAYLIST_ICON("playlistIcon"),

        PLAY_BUTTON("playButton"),
        PREV_BUTTON("prevButton"),
        NEXT_BUTTON("nextButton"),
        PAUSE_BUTTON("pauseButton"),
        NEXT_PLAYLIST_BUTTON("nextPlaylistButton"),
        PLAYLIST_BUTTON("playlistButton"),
        PREV_PLAYLIST_BUTTON("prevPlaylistButton"),
        REMOVE_PLAYLIST_FROM_DISK_BUTTON("removePlaylistFromDiskButton"),
        REMOVE_PLAYLIST_FROM_APP_BUTTON("removePlaylistFromAppButton"),

        HIDE_LEFT_BUTTON("hideLeftButton"),
        HIDE_RIGHT_BUTTON("hideRightButton"),
        COMMONS("commons"),

        LIGHTAUDIO("lightaudio"),
        HITMOS("hitmos"),
        MUSMORE("musmore"),
        LASTFM("lastfm"),
        SPOTIFY("spotify"),
        SOUND_CLOUD("sound_cloud"),
        APPLE("apple"),

        FONT("font"),
        MAIN_FONT("main_font"),

        CONFIG_DIR_PATH("configDirPath"),
        CONFIG_PATH("configPath"),
        HOTKEYS("hotkeys"),

        DEFAULT_CACHE_PATH("cache"),
        DEFAULT_LOCAL_TRACKS_CACHE_PATH(DEFAULT_CACHE_PATH.getKey() + File.separator + "cache"),
        DEFAULT_PLAYLIST_CACHE_PATHS(DEFAULT_LOCAL_TRACKS_CACHE_PATH.getKey() + File.separator + "playlists"),
        DEFAULT_CACHE_TRACKS_PATH(DEFAULT_LOCAL_TRACKS_CACHE_PATH.getKey() + File.separator + "tracks"),
        DEFAULT_CACHE_TRACKS_TAGS_PATH(DEFAULT_CACHE_TRACKS_PATH.getKey() + File.separator + "tags"),
        DEFAULT_INET_CACHE_PATH(DEFAULT_CACHE_PATH.getKey() + File.separator + "inet"),
        DEFAULT_INET_TRACKS_CACHE_PATH(DEFAULT_INET_CACHE_PATH.getKey() + File.separator + "tracks.txt"),
        DEFAULT_COMMON_CACHE_PATH(DEFAULT_CACHE_PATH.getKey() + File.separator + "shared.txt"),

        HISTORY_FILE_PATH(DEFAULT_LOCAL_TRACKS_CACHE_PATH.getKey() + File.separator + "history.txt"),

        AUDIO("audio"),
        AUDIO_INTRO("audio_intro"),

        LANG("lang"),
        PLUGINS_PATH("pluginsPath");

        private final String key;

        Properties(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}