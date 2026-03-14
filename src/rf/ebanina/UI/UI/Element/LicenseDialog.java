package rf.ebanina.UI.UI.Element;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import rf.ebanina.File.Resources.ResourceManager;

public class LicenseDialog
        extends Dialog
{
    private final ScrollPane scrollPane;
    private final rf.ebanina.UI.UI.Element.Buttons.Button acceptButton;

    public LicenseDialog(Stage ownerStage, String title, String licenseText, String btnText) {
        super(ownerStage);

        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");

        Text content = new Text(licenseText);
        content.setFill(Color.LIGHTGRAY);
        content.setStyle("-fx-font-size: 14px;");
        TextFlow flow = new TextFlow(content);
        flow.setLineSpacing(5);

        scrollPane = new ScrollPane(flow);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.getStylesheets().add(ResourceManager.Instance.loadStylesheet("scrollpane-scroll-bar"));

        Label btnLabel = new Label(btnText);
        btnLabel.setTextFill(Color.WHITE);

        acceptButton = new rf.ebanina.UI.UI.Element.Buttons.Button(btnLabel) {};
        acceptButton.setCornerRadius(20);
        acceptButton.setSize(140, 44);
        acceptButton.setDisable(true);
        acceptButton.setCursor(Cursor.HAND);

        acceptButton.setOnAction(e -> hide());

        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 0.98) {
                acceptButton.setDisable(false);
            }
        });

        Platform.runLater(() -> {
            if (scrollPane.getVmax() == 0 || flow.getBoundsInLocal().getHeight() < scrollPane.getHeight()) {
                acceptButton.setDisable(false);
            }
        });

        dialogBox.getChildren().addAll(titleLabel, scrollPane, acceptButton);
    }
}
