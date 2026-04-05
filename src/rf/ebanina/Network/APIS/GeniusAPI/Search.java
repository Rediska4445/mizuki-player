package rf.ebanina.Network.APIS.GeniusAPI;

import org.json.simple.parser.ParseException;
import rf.ebanina.UI.Root;
import rf.ebanina.utils.network.Request;
import rf.ebanina.utils.network.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

public class Search {
    private static String URL = "https://genius.com/api/search/multi";
    private static int LIMIT = 1;

    public static GeniusTrack Search(String What) throws IOException, ParseException {
        Request request = new Request(new URL(URL + "?per_page=" + LIMIT + "&q=" + URLEncoder.encode(What)));
        Response response = request.send();

        return GeniusJsonProcess.getTrackForSearch(response.getBody().toString());
    }

    public static void main(String[] args) throws IOException, ParseException {
        System.out.println(Search("dvrst - close eyes"));

        System.out.println(getLyrics("dvrst - close eyes"));
    }

    public static StringBuilder getLyrics(String What) {
        try {
            GeniusTrack track = Search(What);

            return GeniusJsonProcess.getTrackLyrics(track);
        } catch (IOException | ParseException e) {
            e.printStackTrace();

            throw new RuntimeException(e);
        }
    }

    public static void openGeniusLyrics(String what) {
        new Thread(() -> {
            try {
                Root.rootImpl.openBrowser(new URI(Search.Search(what).getUrl()));
            } catch (URISyntaxException | IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
