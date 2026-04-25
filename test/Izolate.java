import javafx.application.Application;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Izolate {
    public static void main(String[] args) {
        String apiKey = "52ad98fbc4ea0816d852a1ce0598b740";

        String artist = URLEncoder.encode("Moondeity", StandardCharsets.UTF_8);
        String track = URLEncoder.encode("Neon Blade", StandardCharsets.UTF_8);

        String url = "https://ws.audioscrobbler.com/" +
                "?method=track.getsimilar" +
                "&artist=" + artist +
                "&track=" + track +
                "&api_key=" + apiKey +
                "&format=json";

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL) // На случай редиректов
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "JavaApp")
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(System.out::println)
                .join();
    }
}