package rf.ebanina.Network.APIS.SoundCloudAPI;

import org.json.simple.parser.ParseException;
import rf.ebanina.Network.APIS.SoundCloud;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

public class RelatedTracks {
    private int LIMIT = 25;
    private final String RAW_SEARCH_LINK = "https://api-v2.soundcloud.com/tracks/";
    public int getLIMIT() {
        return LIMIT;
    }

    public RelatedTracks setLIMIT(int LIMIT) {
        this.LIMIT = LIMIT;
        return this;
    }

    public List<SoundCloudTrack> getRelatedTracks(SoundCloudTrack What) throws IOException, ParseException {
        Request request = new Request(new URL(RAW_SEARCH_LINK + URLEncoder.encode(What.getId()) + "/related?" + SoundCloud.CLIENT_ID + SoundCloud.SEPARATOR + SoundCloud.LIMIT_STR + LIMIT));
        Response response = request.send();

        return new SoundCloudJsonProcess().getTrackForRelatedTracks(response.getBody().toString());
    }
}
