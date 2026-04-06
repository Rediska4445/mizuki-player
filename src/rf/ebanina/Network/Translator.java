package rf.ebanina.Network;

import javafx.application.Platform;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.FileManager;
import rf.ebanina.UI.Root;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Translator {
    public static Translator instance = new Translator();

    public Translator() {
    }

    private final String translate_type = FileManager.instance.splitData(ConfigurationManager.instance.getItem("translate_type", "libre"));

    public ArrayList<String> yandexTranslate(String in) {
        ArrayList<String> res = new ArrayList<>();

        try {
            for(JSONObject a : getTabs(getResult(getResponse(in)))) {
                if(a.get("text") != null) {
                    res.add(a.get("text").toString());
                }
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return res;
    }

    public String TranslateNodeText(String text, String localeTo) {
        AtomicReference<String> texts = new AtomicReference<>();

        new Thread(() -> {
            if (translate_type.equals("yandex")) {
                texts.set(yandexTranslate(URLEncoder.encode(text, StandardCharsets.UTF_8)).get(0));
            } else {
                try {
                    texts.set(unescapeUnicode(translate(text, localeTo)));
                } catch (Exception e) {}
            }

            final String finalTexts = texts.get();

            Platform.runLater(() -> {
                Root.rootImpl.currentTrackName.setText(finalTexts);
            });
        }).start();

        return texts.get();
    }

    public String translate(String text, String targetLang) throws Exception {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String urlStr = String.format(
                "https://api.mymemory.translated.net/get?q=%s&langpair=en|%s",
                encodedText, targetLang
        );

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP request failed with code " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        String json = response.toString();

        String marker = "\"translatedText\":\"";
        int start = json.indexOf(marker);
        if (start == -1) {
            throw new RuntimeException("Перевод не найден в ответе: " + json);
        }
        start += marker.length();
        int end = json.indexOf("\"", start);
        if (end == -1) {
            throw new RuntimeException("Ошибка парсинга JSON ответа: " + json);
        }

        String translated = json.substring(start, end);

        translated = translated.replace("\\n", "\n").replace("\\\"", "\"");

        return translated;
    }

    public String unescapeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length;) {
            char c = input.charAt(i++);
            if (c == '\\' && i < length && input.charAt(i) == 'u') {
                i++;
                if (i + 4 <= length) {
                    String hex = input.substring(i, i + 4);
                    i += 4;
                    try {
                        int code = Integer.parseInt(hex, 16);
                        sb.append((char) code);
                    } catch (NumberFormatException e) {
                        sb.append("\\u").append(hex);
                    }
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String getResponse(String src) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL("https://dictionary.yandex.net/dicservice.json/queryCorpus?srv=tr-text&sid=23d04c98.6803baae.8bb9f5fe.74722d74657874&ui=ru&src=" +
                    src + "&lang=en-ru&flags=1063&options=226&chunks=1&maxlen=200&v=2&yu=2855030421742154479&yum=1742164253612078805");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream (
                    connection.getOutputStream());
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }


    private String getResult(String req) throws ParseException {
        Object obj2 = new JSONParser().parse(req);
        JSONObject jsonObject1 = (JSONObject) obj2;

        String result = jsonObject1.get("result").toString();

        Object obj3 = new JSONParser().parse(result);
        JSONObject jsonObject2 = (JSONObject) obj3;

        return jsonObject2.get("tabs").toString();
    }

    private ArrayList<JSONObject> getTabs(String tabsJSON) throws ParseException {
        ArrayList<JSONObject> res = new ArrayList<>();

        Object obj4 = new JSONParser().parse(tabsJSON);
        JSONArray jsonObject3 = (JSONArray) obj4;

        for(Object a : jsonObject3) {

            String translation = ((JSONObject) a).get("translation").toString();

            Object obj6 = new JSONParser().parse(translation);
            JSONObject jsonObject5 = (JSONObject) obj6;

            res.add(jsonObject5);
        }

        return res;
    }
}