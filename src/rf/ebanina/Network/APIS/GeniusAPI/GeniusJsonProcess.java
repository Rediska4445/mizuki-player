package rf.ebanina.Network.APIS.GeniusAPI;

import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import rf.ebanina.utils.network.Request;
import rf.ebanina.utils.network.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static rf.ebanina.utils.formats.json.JsonProcess.getJsonArray;
import static rf.ebanina.utils.formats.json.JsonProcess.getJsonItem;

public class GeniusJsonProcess
{
    public static GeniusTrack getTrackForSearch(String rawJson) throws ParseException {
        String collection_item_arr = getJsonItem(rawJson, "response");
        String sections_item_arr = getJsonItem(collection_item_arr, "sections");
        String response = getJsonArray(sections_item_arr)[0];
        String hits = getJsonItem(response, "hits");
        String hits_arr = getJsonArray(hits)[0];
        String result = getJsonItem(hits_arr, "result");

        return new GeniusTrack()
                .setUrl(getJsonItem(result, "url"))
                .setArtist(getJsonItem(result, "artist_names"))
                .setTitle(getJsonItem(result, "title"))
                .setId(getJsonItem(result, "id"));
    }

    public static StringBuilder getTrackLyrics(GeniusTrack track) {
        Request request;

        try {
            request = new Request(new URL(track.getUrl()));
        } catch (MalformedURLException e) {
            e.printStackTrace();

            throw new RuntimeException(e);
        }

        Response respa;

        try {
            respa = request.send();
        } catch (IOException e) {
            e.printStackTrace();

            throw new RuntimeException(e);
        }

        System.out.println(respa.getBody().toString());

        return extractSubtitlesFromHtml(respa.getBody().toString());
    }

    private static StringBuilder extractSubtitlesFromHtml(String html) {
        Document doc = Jsoup.parse(html);

        Elements lyricsContainers = doc.select("div[data-lyrics-container=true]");
        if (lyricsContainers.isEmpty()) {
            return new StringBuilder();
        }

        StringBuilder lyricsBuilder = new StringBuilder();

        for (Element container : lyricsContainers) {
            appendNodeTextWithNewlines(container, lyricsBuilder);
            lyricsBuilder.append("\n");
        }

        return lyricsBuilder;
    }

    private static void appendNodeTextWithNewlines(Node node, StringBuilder builder) {
        for (Node child : node.childNodes()) {
            if (child.nodeName().equals("br")) {
                builder.append("\n");
            } else if (child instanceof TextNode) {
                builder.append(((TextNode) child).text());
            } else {
                appendNodeTextWithNewlines(child, builder);
            }
        }
    }
}
