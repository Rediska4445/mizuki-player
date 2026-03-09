package rf.ebanina.Network.APIS.SoundCloudAPI;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import rf.ebanina.Network.APIS.JsonProcess;

import java.util.ArrayList;
import java.util.List;

public final class SoundCloudJsonProcess extends JsonProcess {
    static String title = "title";
    static String artist = "username";
    static String duration = "duration";
    private static String user = "user";
    static String createAt = "created_at";
    static String description = "description";
    static String playback_count = "playback_count";
    static String id = "id";
    static String permalink_url = "permalink_url";
    static String art_work = "artwork_url";
    static String comments_counts = "comment_count";
    static String genre = "genre";
    private static String likes_count = "likes_count";

    public SoundCloudTrack getTrackForSearch(String rawJson) throws ParseException {
        String collection_item_arr = getJsonItem(rawJson, "collection");
        String collection = getJsonArray(collection_item_arr)[0];

        return getTrackOfCollection(collection);
    }

    private SoundCloudTrack getTrackOfCollection(String collection) throws ParseException {
        SoundCloudTrack result = new SoundCloudTrack();

        result
                .setId(getJsonItem(collection, id))
                .setTitle(getJsonItem(collection, title))
                .setArtwork(getJsonItem(collection, art_work))
                .setDura(getJsonItem(collection, duration))
                .setPlaybackCount(getJsonItem(collection, playback_count))
                .setCreateAt(getJsonItem(collection, createAt))
                .setLikesCount(getJsonItem(collection, likes_count))
                .setDescription(getJsonItem(collection, description))
                .setLink(getJsonItem(collection, permalink_url))
                .setCommentCounts(getJsonItem(collection, comments_counts))
                .setGenre(getJsonItem(collection, genre))
                .setArtist(getJsonItem(getJsonItem(collection, user), artist));

        if(getJsonItem(collection, description) == null) {
            result.setDescription("null");
        }

        return result;
    }

    public List<rf.ebanina.Network.APIS.SoundCloudAPI.SoundCloudTrack> getTrackForRelatedTracks(String rawJson) throws ParseException {
        String collection_item_arr = getJsonItem(rawJson, "collection");

        JSONArray collections_arr = getRawArray(collection_item_arr);

        List<rf.ebanina.Network.APIS.SoundCloudAPI.SoundCloudTrack> res = new ArrayList<>();

        for(int i = 0; i < collections_arr.size(); i++) {
            res.add(getTrackOfCollection(collections_arr.get(i).toString()));
        }

        return res;
    }
}