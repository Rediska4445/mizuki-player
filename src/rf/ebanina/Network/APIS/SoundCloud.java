package rf.ebanina.Network.APIS;

import org.json.simple.parser.ParseException;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.Network.APIS.SoundCloudAPI.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static rf.ebanina.Network.APIS.SoundCloudAPI.Search.getSearch;

public class SoundCloud {
    public Search searchCore = new Search();
    public RelatedTracks relatedTracksCore = new RelatedTracks();

    public SoundCloudTrack Search(String What) throws IOException, ParseException {
        return searchCore.Search(What);
    }

    public SoundCloudTrack SearchWithoutExceptions(String What) {
        try {
            return searchCore.Search(What);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SoundCloudTrack> getRelatedTrack(SoundCloudTrack track) throws IOException, ParseException {
        return relatedTracksCore.getRelatedTracks(track);
    }

    public static String CLIENT_ID;
    public static final String SEPARATOR = "&";
    public static final String LIMIT_STR = "limit=";

    public static SoundCloud getSoundCloud() {
        return new SoundCloud();
    }

    public SoundCloud() {
        if(CLIENT_ID == null) {
            CLIENT_ID = "client_id=" + ConfigurationManager.instance.getItem("soundcloud_api_client_id", "");
        }

        if(ConfigurationManager.instance.getItem("soundcloud_api_client_id", "").equalsIgnoreCase("null")) {
            try {
                CLIENT_ID = updateClientId();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws IOException, ParseException {
        System.out.println(getSoundCloud()
                .getRelatedTrack(
                        getSearch().Search("kute - avoid me")
                ));
    }

    public String updateClientId() throws IOException {
        String clientId;

        Request request = new Request(new URL("https://a-v2.sndcdn.com/assets/0-cbfed2d8.js"));
        Response response = request.send();

        String body = response.getBody().toString();

        String clientIdName = "client_id=";

        int clientIdIndex = body.indexOf(clientIdName);

        clientId = body.substring(clientIdIndex + clientIdName.length(), body.indexOf("\"", clientIdIndex));

        return "client_id=" + clientId;
    }

    @Override
    public String toString() {
        return "API{" +
                "CLIENT_ID='" + CLIENT_ID + '\'' +
                ", SEPARATOR='" + SEPARATOR + '\'' +
                ", LIMIT_STR='" + LIMIT_STR + '\'' +
                '}';
    }
}
