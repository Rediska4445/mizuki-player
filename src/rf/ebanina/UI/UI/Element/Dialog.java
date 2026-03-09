package rf.ebanina.UI.UI.Element;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import rf.ebanina.File.Resources.ResourceManager;

public class AgreementDialog extends StackPane {
    private final Region backgroundDim;
    private final VBox dialogBox;
    private final ScrollPane scrollPane;
    private final TextArea agreementTextArea;
    private final rf.ebanina.UI.UI.Element.Buttons.Button acceptButton;

    private double widthPercent = 0.8;
    private double heightPercent = 0.8;

    private Runnable onAgree;

    public AgreementDialog setOnAgree(Runnable onAgree) {
        this.onAgree = onAgree;
        return this;
    }

    public AgreementDialog(Stage ownerStage, String agreeText, String agreementText) {
        backgroundDim = new Region();
        backgroundDim.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        backgroundDim.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        agreementTextArea = new TextArea(agreementText);
        agreementTextArea.setWrapText(true);
        agreementTextArea.setEditable(false);
        agreementTextArea.setFocusTraversable(false);
        agreementTextArea.setPrefWidth(600);
        agreementTextArea.setPrefHeight(400);
        agreementTextArea.setStyle("-fx-font-size: 14px;");

        scrollPane = new ScrollPane(agreementTextArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        Label label = new Label(agreeText);
        label.setTextFill(Color.WHITE);

        acceptButton = new rf.ebanina.UI.UI.Element.Buttons.Button(label) {};
        acceptButton.setCornerRadius(5);
        acceptButton.setSize(100, 40);
        acceptButton.setDisable(false);

        acceptButton.setOnAction(e -> {
            this.setVisible(false);

            onAgree.run();
        });

        dialogBox = new VBox(15, scrollPane, acceptButton);
        dialogBox.setAlignment(Pos.CENTER);

        double w = ownerStage.getWidth() * widthPercent;
        double h = ownerStage.getHeight() * heightPercent;
        dialogBox.setPrefSize(w, h);
        scrollPane.setPrefHeight(h - 60);

        this.setPrefSize(ownerStage.getWidth(), ownerStage.getHeight());
        this.getChildren().addAll(backgroundDim, dialogBox);

        StackPane.setAlignment(dialogBox, Pos.CENTER);
        StackPane.setAlignment(backgroundDim, Pos.CENTER);

        backgroundDim.prefWidthProperty().bind(this.prefWidthProperty());
        backgroundDim.prefHeightProperty().bind(this.prefHeightProperty());

        dialogBox.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.35),
                new CornerRadii(10, 10, 0, 0, false),
                Insets.EMPTY
        )));

        dialogBox.setBorder(new Border(new BorderStroke(
                Color.GRAY,
                BorderStrokeStyle.SOLID,
                new CornerRadii(10, 10, 0, 0, false),
                new BorderWidths(1,1,0,1)
        )));

        acceptButton.setTextFill(Color.WHITE);
        acceptButton.setCursor(Cursor.HAND);

        acceptButton.setBackground(new Background(new BackgroundFill(
                Color.web("#212121"),
                new CornerRadii(0, 0, 0, 0, false),
                Insets.EMPTY
        )));

        agreementTextArea.setBackground(new Background(new BackgroundFill(
                Color.web("#212121"),
                new CornerRadii(0, 0, 0, 0, false),
                Insets.EMPTY
        )));

        acceptButton.setDisable(true);

        agreementTextArea.scrollTopProperty().addListener((obs, oldVal, newVal) -> {
            Node textFlow = agreementTextArea.lookup(".text");

            if (textFlow instanceof Text) {
                double totalHeight = textFlow.getBoundsInLocal().getHeight();
                double viewportHeight = agreementTextArea.getHeight();
                double scrollTop = agreementTextArea.getScrollTop();

                acceptButton.setDisable(!(scrollTop + viewportHeight >= totalHeight - 10));
            }
        });

        setBorder(new Border(new BorderStroke(
                Color.GRAY,
                BorderStrokeStyle.SOLID,
                new CornerRadii(10, 10, 10, 10, false),
                new BorderWidths(1,1,1,1)
        )));

        agreementTextArea.setStyle("-fx-control-inner-background: #212121;");
        agreementTextArea.getStylesheets().add(ResourceManager.Instance.loadStylesheet("scrollpane-scroll-bar"));
    }

    public void show() {
        this.setVisible(true);
    }

    public FadeTransition hide() {
        FadeTransition fade = new FadeTransition(Duration.millis(500), this);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(event -> {
            this.setVisible(false);
            this.setOpacity(1.0);
        });

        fade.play();

        return fade;
    }
}
