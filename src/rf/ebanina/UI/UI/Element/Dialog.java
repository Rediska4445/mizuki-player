package rf.ebanina.UI.UI.Element;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import rf.ebanina.File.Resources.ResourceManager;

public class Dialog extends StackPane {
    private final Region backgroundDim;
    private final VBox dialogBox;
    private final ScrollPane scrollPane;
    private final TextFlow textFlow;
    private final rf.ebanina.UI.UI.Element.Buttons.Button acceptButton;

    private Runnable onAgree;

    public Dialog setOnAgree(Runnable onAgree) {
        this.onAgree = onAgree;
        return this;
    }

    public Dialog(Stage ownerStage, String agreeText, String agreementText) {
        // Затемнение фона
        backgroundDim = new Region();
        backgroundDim.setStyle("-fx-background-color: rgba(0, 0, 0, 0.55);");
        backgroundDim.setOpacity(0);

        // Контент (текст)
        Text content = new Text(agreementText);
        content.setFill(Color.LIGHTGRAY);
        content.setStyle("-fx-font-size: 14px;");

        textFlow = new TextFlow(content);
        textFlow.setPadding(new Insets(20));
        textFlow.setLineSpacing(5);

        scrollPane = new ScrollPane(textFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // Прозрачный фон скролла в стиле Material
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Label label = new Label(agreeText);
        label.setTextFill(Color.WHITE);

        acceptButton = new rf.ebanina.UI.UI.Element.Buttons.Button(label) {};
        acceptButton.setCornerRadius(20); // Более скругленные кнопки как в новом Google Style
        acceptButton.setSize(140, 40);
        acceptButton.setDisable(true);
        acceptButton.setCursor(Cursor.HAND);
        acceptButton.setBackground(new Background(new BackgroundFill(Color.web("#212121"), new CornerRadii(20), Insets.EMPTY)));

        acceptButton.setOnAction(e -> hide());

        // Контейнер окна
        dialogBox = new VBox(20, scrollPane, acceptButton);
        dialogBox.setAlignment(Pos.CENTER);
        dialogBox.setPadding(new Insets(25));
        dialogBox.setMaxSize(ownerStage.getWidth() * 0.8, ownerStage.getHeight() * 0.8);

        dialogBox.setBackground(new Background(new BackgroundFill(
                Color.web("#212121"),
                new CornerRadii(12),
                Insets.EMPTY
        )));

        dialogBox.setBorder(new Border(new BorderStroke(
                Color.GRAY, BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1)
        )));

        // Логика разблокировки кнопки при прокрутке
        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 0.98) {
                acceptButton.setDisable(false);
            }
        });

        this.getChildren().addAll(backgroundDim, dialogBox);
        this.setVisible(false);

        scrollPane.getStylesheets().add(ResourceManager.Instance.loadStylesheet("scrollpane-scroll-bar"));
    }

    public void show() {
        this.setVisible(true);

        // Анимация Google Style: Плавное появление + легкое масштабирование вверх
        FadeTransition fadeInDim = new FadeTransition(Duration.millis(300), backgroundDim);
        fadeInDim.setToValue(1.0);

        FadeTransition fadeInBox = new FadeTransition(Duration.millis(300), dialogBox);
        fadeInBox.setFromValue(0.0);
        fadeInBox.setToValue(1.0);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), dialogBox);
        scaleIn.setFromX(0.85);
        scaleIn.setFromY(0.85);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        ParallelTransition showAnim = new ParallelTransition(fadeInDim, fadeInBox, scaleIn);
        showAnim.play();
    }

    public ParallelTransition hide() {
        // Анимация исчезновения: Плавное затухание + уменьшение
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setToValue(0.0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), dialogBox);
        scaleOut.setToX(0.95);
        scaleOut.setToY(0.95);

        ParallelTransition hideAnim = new ParallelTransition(fadeOut, scaleOut);
        hideAnim.setOnFinished(e -> {
            this.setVisible(false);
            this.setOpacity(1.0);
            if (onAgree != null) onAgree.run();
        });

        hideAnim.play();

        return hideAnim;
    }
}
