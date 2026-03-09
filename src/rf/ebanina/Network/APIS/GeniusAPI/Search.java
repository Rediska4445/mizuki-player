package rf.ebanina.Network.APIS.GeniusAPI;

import org.json.simple.parser.ParseException;
import rf.ebanina.UI.Root;
import rf.ebanina.Network.APIS.SoundCloudAPI.Request;
import rf.ebanina.Network.APIS.SoundCloudAPI.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

public class Search {
    private String URL = "https://genius.com/api/search/multi";
    private int LIMIT = 1;

    public GeniusTrack Search(String What) throws IOException, ParseException {
        Request request = new Request(new URL(URL + "?per_page=" + LIMIT + "&q=" + URLEncoder.encode(What)));

        Response response = request.send();

        return new GeniusJsonProcess().getTrackForSearch(response.getBody().toString());
    }

    public static StringBuilder getLyrics(String What) {
        try {
            GeniusTrack track = new Search().Search(What);

            return new GeniusJsonProcess().getTrackLyrics(track);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static void openGeniusLyrics(String what) {
        new Thread(() -> {
            try {
                Root.rootImpl.openBrowser(new URI(new Search().Search(what).getUrl()));
            } catch (URISyntaxException | IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
