package rf.ebanina.File.Localization;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class JsonLocalizationManager
        extends LocalizationManager
{
    private final JSONParser parser = new JSONParser();

    public JsonLocalizationManager(FileManager fileManager, ResourceManager resourceManager, String lang, Path langPath) {
        super(fileManager, resourceManager, lang, langPath);
    }

    private HashMap<String, String> parseJsonToMap(String rawJson) throws ParseException {
        Object obj = parser.parse(rawJson);

        if (!(obj instanceof JSONObject)) {
            throw new IllegalArgumentException("JSON должен быть объектом { ... }");
        }

        HashMap<String, String> map = new HashMap<>();
        addItemsFromObject((JSONObject) obj, "", map);
        return map;
    }

    private void addItemsFromObject(JSONObject obj, String prefix, HashMap<String, String> map) {
        for (Object keyObj : obj.keySet()) {
            String key = keyObj.toString();
            Object value = obj.get(key);

            if (value instanceof JSONObject) {
                addItemsFromObject((JSONObject) value, key, map);
            } else {
                map.put(key, value != null ? value.toString() : null);
            }
        }
    }

    @Override
    protected void putLocale(String word, String ifResultIsNull) {
        try {
            if (localeMap().size() == 0) {
                localeMap().putAll(parseJsonToMap(Files.readString(Path.of(ResourceManager.getInstance().resourcesPaths.get("lang") + File.separator + lang + ".json"))));
            }

            localeMap().putIfAbsent(word, ifResultIsNull);
        } catch (RuntimeException | ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
