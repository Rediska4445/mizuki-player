package rf.ebanina.Network.APIS.SoundCloudAPI;

import org.json.simple.parser.ParseException;
import rf.ebanina.Network.APIS.SoundCloud;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

public class Search {
    private int LIMIT = 25;
    private final String RAW_SEARCH_LINK = "https://api-v2.soundcloud.com/search?q=";

    public int getLIMIT() {
        return LIMIT;
    }

    public static Search getSearch() {
        return new Search();
    }

    public Search setLIMIT(int LIMIT) {
        this.LIMIT = LIMIT;
        return this;
    }

    public SoundCloudTrack Search(String What) throws IOException, ParseException {
        Request request = new Request(new URL(RAW_SEARCH_LINK + URLEncoder.encode(What) + SoundCloud.SEPARATOR + SoundCloud.CLIENT_ID + SoundCloud.SEPARATOR + SoundCloud.LIMIT_STR + LIMIT));
        Response response = request.send();

        return new SoundCloudJsonProcess().getTrackForSearch(response.getBody().toString());
    }

    @Override
    public String toString() {
        return "SearchProcessor{" +
                "LIMIT=" + LIMIT +
                ", RAW_SEARCH_LINK='" + RAW_SEARCH_LINK + '\'' +
                '}';
    }
}