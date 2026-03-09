package rf.ebanina.Network.APIS.SoundCloudAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SoundCloudTrack {
    private String artist;
    private String title;
    private String createAt;
    private String description;
    private String dura;
    private String playbackCount;
    private String link;
    private String artwork;
    private String id;
    private String commentCounts;
    private String genre;
    private String likesCount;

    public Map<String, String> getInfo() {
        Map<String, String> res = new HashMap<>();

        res.put(SoundCloudJsonProcess.artist, getArtist());
        res.put(SoundCloudJsonProcess.title, getTitle());
        res.put(SoundCloudJsonProcess.createAt, getCreateAt());
        res.put(SoundCloudJsonProcess.description, getDescription());
        res.put(SoundCloudJsonProcess.duration, getDura());
        res.put(SoundCloudJsonProcess.playback_count, getPlaybackCount());
        res.put(SoundCloudJsonProcess.permalink_url, getLink());
        res.put(SoundCloudJsonProcess.art_work, getArtwork());
        res.put(SoundCloudJsonProcess.id, getId());
        res.put(SoundCloudJsonProcess.comments_counts, getCommentCounts());
        res.put(SoundCloudJsonProcess.genre, getGenre());

        return res;
    }

    public String getLikesCount() {
        return likesCount;
    }

    public SoundCloudTrack setLikesCount(String likesCount) {
        this.likesCount = likesCount;
        return this;
    }

    public String getGenre() {
        return genre;
    }

    public SoundCloudTrack setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public String getCommentCounts() {
        return commentCounts;
    }

    public SoundCloudTrack setCommentCounts(String commentCounts) {
        this.commentCounts = commentCounts;
        return this;
    }

    public String getArtwork() {
        return artwork;
    }

    public SoundCloudTrack setArtwork(String artwork) {
        this.artwork = artwork;
        return this;
    }

    public String getLink() {
        return link;
    }

    public SoundCloudTrack setLink(String link) {
        this.link = link;
        return this;
    }

    public String getCreateAt() {
        return createAt;
    }

    public SoundCloudTrack setCreateAt(String createAt) {
        this.createAt = createAt;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SoundCloudTrack setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDura() {
        return dura;
    }

    public SoundCloudTrack setDura(String dura) {
        this.dura = dura;
        return this;
    }

    public String getPlaybackCount() {
        return playbackCount;
    }

    public SoundCloudTrack setPlaybackCount(String playbackCount) {
        this.playbackCount = playbackCount;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public SoundCloudTrack setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public SoundCloudTrack setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getId() {
        return id;
    }

    public SoundCloudTrack setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String toString() {
        return "SoundCloudTrack{" +
                "artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", createAt='" + createAt + '\'' +
                ", description='" + description + '\'' +
                ", dura='" + dura + '\'' +
                ", playbackCount='" + playbackCount + '\'' +
                ", link='" + link + '\'' +
                ", artwork='" + artwork + '\'' +
                ", id='" + id + '\'' +
                ", commentCounts='" + commentCounts + '\'' +
                ", genre='" + genre + '\'' +
                ", likesCount='" + likesCount + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoundCloudTrack that = (SoundCloudTrack) o;
        return Objects.equals(artist, that.artist) && Objects.equals(title, that.title) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title, id);
    }
}