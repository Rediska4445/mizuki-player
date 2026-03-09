package rf.ebanina.Network.APIS.GeniusAPI;

public class GeniusTrack {
    private String artist;
    private String title;
    private String id;
    private String url;

    public String getUrl() {
        return url;
    }

    public GeniusTrack setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getId() {
        return id;
    }

    public GeniusTrack setId(String id) {
        this.id = id;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public GeniusTrack setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public GeniusTrack setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public String toString() {
        return "GeniusTrack{" +
                "artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", id='" + id + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
