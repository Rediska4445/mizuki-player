package rf.ebanina.Network.Illegal.Similar;

import javafx.application.Platform;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.Network.ISimilar;
import rf.ebanina.Network.Info;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static rf.ebanina.UI.Root.similar;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isPreserveRatio;
import static rf.ebanina.UI.UI.Paint.ColorProcessor.isSmooth;

public class Apple implements ISimilar {
    private static String getAppleHref(String t) {
        try {
            Document a = Jsoup.connect("https://music.apple.com/ru/search?term=" + URLEncoder.encode(t.replace(" ", ""), StandardCharsets.UTF_8)).get();

            if(a.getElementsByClass("click-action svelte-c0t0j2").size() != 0)
                return a.getElementsByClass("click-action svelte-c0t0j2").get(0)
                    .attr("href");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public static ArrayList<Track> getAppleSimilarTracks(String t) {
        ArrayList<Track> tracks = new ArrayList<>();

        try {
            String url = getAppleHref(t);

            if(url != null) {
                String aaa = Jsoup.connect(
                        url
                ).get().getElementById("serialized-server-data").toString();

                String jj = aaa.substring(
                        aaa.indexOf("[") + 1,
                        aaa.indexOf("</script>") - 1
                );

                // Log.println(jj);

                Object obj1 = new JSONParser().parse(jj);
                JSONObject jsonObject = (JSONObject) obj1;

                String data = jsonObject.get("data").toString();

                Object obj2 = new JSONParser().parse(data);
                JSONObject jsonObject1 = (JSONObject) obj2;

                String data2 = jsonObject1.get("sections").toString();

                Object obj3 = new JSONParser().parse(data2);
                JSONArray jsonObject2 = (JSONArray) obj3;

                Object obj4 = new JSONParser().parse(jsonObject2.get(jsonObject2.size() - 1).toString());
                JSONObject jsonObject3 = (JSONObject) obj4;

                String data4 = jsonObject3.get("items").toString();

                Object obj5 = new JSONParser().parse(data4);
                JSONArray jsonObject5 = (JSONArray) obj5;

                for (Object a : jsonObject5) {
                    Object obj51 = new JSONParser().parse(a.toString());
                    JSONObject jsonObject11 = (JSONObject) obj51;

                    JSONArray jsonObject51 = (JSONArray) jsonObject11.get("subtitleLinks");
                    JSONObject jsonObject111 = (JSONObject) jsonObject51.get(0);

                    JSONArray jsonObject511 = (JSONArray) jsonObject11.get("titleLinks");
                    JSONObject jsonObject1111 = (JSONObject) jsonObject511.get(0);

                    String title = jsonObject1111.get("title").toString();
                    String artist = jsonObject111.get("title").toString();

                    Track track = new Track(Info.PlayersTypes.URI_NULL.getCode());
                    track.setExternalUrl(Info.PlayersTypes.APPLE.getCode());

                    track.artist = artist.replace(" - EP", "").replace(" - Single", "");
                    track.title = title.replace(" - EP", "").replace(" - Single", "");

                    track.mipmap = (ResourceManager.Instance.loadImage(Info.PlayersTypes.APPLE.getCode(), 40, 40, isPreserveRatio, isSmooth));
                    track.viewName = track.artist + " - " + track.title;

                    tracks.add(track);
                }
            } else {
                return new ArrayList<>();
            }
        } catch (IOException | ParseException e) {
            return tracks;
        }

        return tracks;
    }

    @Override
    public void updateSimilar(Track track) {
        ArrayList<Track> temp = getAppleSimilarTracks(track.viewName());

        if (temp.size() != 0) {
            temp.addAll(getAppleSimilarTracks(temp.get(0).viewName));

            Platform.runLater(() -> {
                similar.getTrackListView().getItems().addAll(temp);
                Root.PlaylistHandler.playlistSimilar.addAll(temp);
                PlayProcessor.playProcessor.getTracks().addAll(similar.getTrackListView().getItems());
            });
        }
    }

    @Override
    public List<Track> getSimilar(String f) {
        return List.of();
    }
}
