package rf.ebanina.UI.Editors.Player.Tabs.AudioPlugins.Vst;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;

public class VstParamsWindow extends Stage {
    public VstParamsWindow(PluginWrapper plugin) {
        setTitle("Параметры: " + plugin.getVendorName() + " - " + plugin.getProductString());

        VBox paramsBox = new VBox(10);
        paramsBox.setPadding(new Insets(10));
        paramsBox.setFillWidth(true);

        int numParams = plugin.numParameters();
        for (int i = 0; i < numParams; i++) {
            String paramName = plugin.getParameterName(i);
            float value = plugin.getParameter(i);

            Label label = new Label(paramName + " (" + i + ")");
            Slider slider = new Slider(0, 1, value);
            slider.setPrefWidth(220);

            TextField valueField = new TextField(String.format("%.3f", value));
            valueField.setPrefWidth(50);

            int finalI = i;
            slider.valueProperty().addListener((obs, ov, nv) -> {
                float v = nv.floatValue();
                plugin.setParameter(finalI, v);
                valueField.setText(String.format("%.3f", v));
            });

            int finalI1 = i;
            valueField.setOnAction(evt -> {
                try {
                    float newVal = Float.parseFloat(valueField.getText());
                    newVal = Math.min(Math.max(newVal, 0f), 1f);
                    plugin.setParameter(finalI1, newVal);
                    slider.setValue(newVal);
                } catch (Exception ignore) {}
            });

            HBox paramLine = new HBox(10, label, slider, valueField);
            paramLine.setAlignment(Pos.CENTER_LEFT);

            paramsBox.getChildren().add(paramLine);
        }

        ScrollPane scrollPane = new ScrollPane(paramsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(160);

        // Нижняя кнопка "Закрыть"
        Button closeBtn = new Button("Закрыть");
        closeBtn.setPrefWidth(80);
        closeBtn.setOnAction(e -> close());

        AnchorPane root = new AnchorPane();
        root.setPadding(new Insets(6));
        root.getChildren().addAll(scrollPane);

        AnchorPane.setBottomAnchor(scrollPane, 50d);
        AnchorPane.setTopAnchor(scrollPane, 25d);
        AnchorPane.setLeftAnchor(scrollPane, 15d);
        AnchorPane.setRightAnchor(scrollPane, 15d);

        setScene(new Scene(root, 380, 350));
        setResizable(true);
        initModality(Modality.APPLICATION_MODAL);
    }
}
