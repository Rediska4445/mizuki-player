package rf.ebanina.UI.Editors.Settings;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import rf.ebanina.File.Resources.ResourceManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller implements Initializable {

    @FXML
    private TabPane tabPane;

    @FXML
    private Button saveButton;

    private final Map<String, LinkedHashMap<String, String>> configData = new LinkedHashMap<>();

    private final Path configFile = Paths.get(ResourceManager.Instance.resourcesPaths.get("configPath"));

    private void saveConfig() throws IOException {
        configData.clear();

        boolean hasError = false;
        StringBuilder errorMessages = new StringBuilder();

        for (Tab tab : tabPane.getTabs()) {
            String sectionName = tab.getText();
            LinkedHashMap<String, String> sectionMap = new LinkedHashMap<>();

            Node content = tab.getContent();
            if (!(content instanceof ScrollPane))
                continue;

            ScrollPane scroll = (ScrollPane) content;
            Node scrollContent = scroll.getContent();

            if (!(scrollContent instanceof VBox))
                continue;

            VBox vbox = (VBox) scrollContent;

            for (Node node : vbox.getChildren()) {
                if(node instanceof HBox hBox) {
                    for (Node node1 : hBox.getChildren()) {
                        if (node1 instanceof TextField textField) {
                            String key = textField.getId();
                            if (key == null) continue;

                            String text = textField.getText().trim();

                             if (text.isEmpty()) {
                                 text = "";
                            }

                            if (isNumericField(key)) {
                                if (!isValidNumber(text)) {
                                    hasError = true;
                                    errorMessages.append(String.format("Поле \"%s\" в секции \"%s\" должно содержать число.\n",
                                            prettifyKey(key), sectionName));
                                    continue;
                                }
                            }

                            sectionMap.put(key, text);

                        } else if (node1 instanceof CheckBox checkBox) {
                            String key = checkBox.getId();
                            if (key == null) continue;

                            sectionMap.put(key, Boolean.toString(checkBox.isSelected()));
                        }
                    }
                }
            }

            configData.put(sectionName, sectionMap);
        }

        if (hasError) {
            showError("Ошибки валидации", errorMessages.toString());
            return;
        }

        writeConfigToFile(configData, configFile);

        showInfo("Сохранение настроек", "Настройки успешно сохранены.");
    }

    private void writeConfigToFile(Map<String, LinkedHashMap<String, String>> data, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Map.Entry<String, LinkedHashMap<String, String>> sectionEntry : data.entrySet()) {
                writer.write(String.format("<---------------------%s--------------------->%n", sectionEntry.getKey()));

                for (Map.Entry<String, String> param : sectionEntry.getValue().entrySet()) {
                    writer.write(String.format("%s = %s;%n", param.getKey(), param.getValue()));
                }
                writer.write(System.lineSeparator());
            }
        }
    }

    private boolean isNumericField(String key) {
        String lower = key.toLowerCase();
        return lower.contains("size") || lower.contains("width") || lower.contains("height") ||
                lower.contains("time") || lower.contains("delay") || lower.contains("count") ||
                lower.contains("radius") || lower.contains("sleep") || lower.contains("timeout");
    }

    private boolean isValidNumber(String text) {
        // Можно расширить проверку под int, double и т.д.
        try {
            Double.parseDouble(text.replace(',', '.')); // учитываем запятую как десятичный разделитель
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String prettifyKey(String key) {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveButton.setOnAction(e -> {
            try {
                saveConfig();
            } catch (IOException ex) {
                showError("Ошибка записи файла", ex.getMessage());
            }
        });

        try {
            loadConfig();
        } catch (IOException e) {
            showError("Ошибка загрузки конфигурации", e.getMessage());
        }
    }

    private void loadConfig() throws IOException {
        configData.clear();

        Path configFile = Paths.get(ResourceManager.Instance.resourcesPaths.get("configPath"));
        if (!Files.exists(configFile)) {
            showError("Ошибка", "Файл конфигурации не найден: " + configFile.toAbsolutePath());
            return;
        }

        List<String> lines = Files.readAllLines(configFile);

        String currentSection = null;
        Pattern sectionPattern = Pattern.compile("<-+(.+?)-+>");
        Pattern kvPattern = Pattern.compile("^\\s*([a-zA-Z0-9_]+)\\s*=\\s*(.*);\\s*$");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            Matcher sectionMatcher = sectionPattern.matcher(line);
            if (sectionMatcher.matches()) {
                currentSection = sectionMatcher.group(1).trim();
                configData.put(currentSection, new LinkedHashMap<>());
                continue;
            }

            if (currentSection == null)
                continue;

            Matcher kvMatcher = kvPattern.matcher(line);
            if (kvMatcher.matches()) {
                String key = kvMatcher.group(1).trim();
                String value = kvMatcher.group(2).trim();

                if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1);
                }

                configData.get(currentSection).put(key, value);
            }
        }

        for (Tab tab : tabPane.getTabs()) {
            String sectionName = tab.getText();
            Map<String, String> sectionMap = configData.get(sectionName);
            if (sectionMap == null) continue;

            Node content = tab.getContent();
            if (!(content instanceof ScrollPane)) continue;
            ScrollPane scroll = (ScrollPane) content;
            Node scrollContent = scroll.getContent();
            if (!(scrollContent instanceof VBox)) continue;
            VBox vbox = (VBox) scrollContent;

            for (Node node : vbox.getChildren()) {
                if (node instanceof TextField textField) {
                    String key = textField.getId();
                    if (key == null) continue;

                    String value = sectionMap.get(key);
                    if (value != null) {
                        textField.setText(value);
                    }
                } else if (node instanceof CheckBox checkBox) {
                    String key = checkBox.getId();
                    if (key == null) continue;

                    String value = sectionMap.get(key);
                    if (value != null) {
                        checkBox.setSelected(value.equalsIgnoreCase("true"));
                    }
                }
            }
        }
    }
}
