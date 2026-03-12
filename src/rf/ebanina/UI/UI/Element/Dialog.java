package rf.ebanina.UI.UI.Element;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public abstract class Dialog
        extends StackPane
{
    protected final Pane backgroundDim;
    protected final VBox dialogBox;
    protected Runnable onAction;
    protected final Stage ownerStage;

    public Dialog(Stage ownerStage) {
        this.ownerStage = ownerStage;

        backgroundDim = new Pane();
        backgroundDim.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        backgroundDim.setOpacity(0);
        backgroundDim.setOnMouseClicked(this::hideOnBackgroundClick);

        dialogBox = new VBox(20);
        dialogBox.setAlignment(Pos.CENTER);
        dialogBox.setPadding(new Insets(25));
        dialogBox.setBackground(new Background(new BackgroundFill(
                Color.web("#1E1E1E"), new CornerRadii(16), Insets.EMPTY
        )));
        dialogBox.setOpacity(0);

        dialogBox.setBorder(new Border(new BorderStroke(
                Color.gray(0.3), BorderStrokeStyle.SOLID, new CornerRadii(16), new BorderWidths(1)
        )));

        bindBackgroundToStage();
        this.getChildren().addAll(backgroundDim, dialogBox);
        this.setVisible(false);
    }

    private void bindBackgroundToStage() {
        backgroundDim.prefWidthProperty().bind(ownerStage.widthProperty());
        backgroundDim.prefHeightProperty().bind(ownerStage.heightProperty());
        backgroundDim.minWidthProperty().bind(ownerStage.widthProperty());
        backgroundDim.minHeightProperty().bind(ownerStage.heightProperty());
        backgroundDim.maxWidthProperty().bind(ownerStage.widthProperty());
        backgroundDim.maxHeightProperty().bind(ownerStage.heightProperty());
    }

    /**
     * Устанавливает максимальные размеры диалога (коэффициенты от stage)
     */
    public void setDialogMaxSize(double widthFactor, double heightFactor) {
        dialogBox.setMaxSize(
                ownerStage.getWidth() * widthFactor,
                ownerStage.getHeight() * heightFactor
        );
    }

    /**
     * Устанавливает фиксированные размеры диалога
     */
    public void setDialogSize(double width, double height) {
        dialogBox.setPrefSize(width, height);
        dialogBox.setMaxSize(width, height);
    }

    /**
     * Устанавливает отступы внутри диалога
     */
    public void setDialogPadding(Insets padding) {
        dialogBox.setPadding(padding);
    }

    // === МЕТОДЫ ДЛЯ ФОНА ===

    /**
     * Уровень затемнения фона (0.0 - прозрачный, 1.0 - полностью чёрный)
     */
    public void setBackgroundOpacity(double opacity) {
        opacity = Math.max(0.0, Math.min(1.0, opacity));
        String rgba = String.format("rgba(0, 0, 0, %.2f)", opacity);
        backgroundDim.setStyle("-fx-background-color: " + rgba + ";");
    }

    /**
     * Устанавливает цвет фона (по умолчанию чёрный)
     */
    public void setBackgroundColor(Color color, double opacity) {
        opacity = Math.max(0.0, Math.min(1.0, opacity));
        String rgba = String.format("rgba(%.0f, %.0f, %.0f, %.2f)",
                color.getRed() * 255,
                color.getGreen() * 255,
                color.getBlue() * 255,
                opacity);
        backgroundDim.setStyle("-fx-background-color: " + rgba + ";");
    }

    /**
     * Включает/выключает закрытие по клику на фон
     */
    public void setCloseOnBackgroundClick(boolean enabled) {
        if (enabled) {
            backgroundDim.setOnMouseClicked(this::hideOnBackgroundClick);
        } else {
            backgroundDim.setOnMouseClicked(null);
        }
    }

    /**
     * Устанавливает цвет фона диалога
     */
    public void setDialogBackgroundColor(Color color) {
        dialogBox.setBackground(new Background(new BackgroundFill(
                color, new CornerRadii(16), Insets.EMPTY
        )));
    }

    /**
     * Устанавливает радиус скругления углов
     */
    public void setDialogCornerRadius(double radius) {
        // Требует пересоздания background и border
        dialogBox.setBackground(new Background(new BackgroundFill(
                Color.web("#1E1E1E"), new CornerRadii(radius), Insets.EMPTY
        )));
        dialogBox.setBorder(new Border(new BorderStroke(
                Color.gray(0.3), BorderStrokeStyle.SOLID, new CornerRadii(radius), new BorderWidths(1)
        )));
    }

    /**
     * Устанавливает spacing между элементами в VBox
     */
    public void setDialogSpacing(double spacing) {
        dialogBox.setSpacing(spacing);
    }

    private void hideOnBackgroundClick(MouseEvent event) {
        if (event.getTarget() == backgroundDim) {
            hide();
        }
    }

    public void show() {
        this.setVisible(true);

        FadeTransition fadeInDim = new FadeTransition(Duration.millis(300), backgroundDim);
        fadeInDim.setToValue(1.0);

        FadeTransition fadeInBox = new FadeTransition(Duration.millis(350), dialogBox);
        fadeInBox.setToValue(1.0);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(350), dialogBox);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.LINEAR);

        new ParallelTransition(fadeInDim, fadeInBox, scaleIn).play();
    }

    public void hide() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setToValue(0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), dialogBox);
        scaleOut.setToX(0.9);
        scaleOut.setToY(0.9);

        ParallelTransition hideAnim = new ParallelTransition(fadeOut, scaleOut);
        hideAnim.setOnFinished(e -> {
            this.setVisible(false);
            this.setOpacity(1.0);
            if (onAction != null) onAction.run();
        });
        hideAnim.play();
    }

    public void setOnAction(Runnable onAction) {
        this.onAction = onAction;
    }
}
