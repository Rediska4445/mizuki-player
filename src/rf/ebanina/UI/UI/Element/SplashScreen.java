package rf.ebanina.UI.UI.Element;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import rf.ebanina.File.Resources.ResourceManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public abstract class SplashScreen
{
    private final Stage splashStage;
    private final SmoothProgress progressBar;
    private final rf.ebanina.UI.UI.Element.Text.Label statusLabel;
    private final rf.ebanina.UI.UI.Element.Text.Label percentLabel;
    private Timeline progressTimeline;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SplashScreen(List<String> funnyTexts) {

        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);

        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        splashStage.setX((primScreenBounds.getWidth() - 500) / 2);
        splashStage.setY((primScreenBounds.getHeight() - 450) / 2);

        ImageView splashImage = new ImageView(
                ResourceManager.getInstance().loadImage("logoHd", 1000, 1000, true, true)
        );
        splashImage.setFitWidth(400);
        splashImage.setPreserveRatio(true);

        percentLabel = new rf.ebanina.UI.UI.Element.Text.Label("0 %");
        percentLabel.setStyle("-fx-text-fill: #888888;");
        percentLabel.setFont(ResourceManager.getInstance().loadFont("main_font", 14));

        statusLabel = new rf.ebanina.UI.UI.Element.Text.Label(funnyTexts.get(0));
        statusLabel.setStyle("-fx-text-fill: white;");
        statusLabel.setFont(ResourceManager.getInstance().loadFont("main_font", 14));

        progressBar = new SmoothProgress(0);
        progressBar.setPrefWidth(480);
        progressBar.setStyle(
                "-fx-indeterminate-bar-length: 60;" +
                        "-fx-background-color: #0f0f0f;" +
                        "-fx-background-insets: 0;" +
                        "-fx-background-radius: 2;" +
                        "-fx-accent: #4a4a4a;" +
                        "-fx-control-inner-background: #0f0f0f;"
        );

        progressBar.getBar().progressProperty().addListener((obs, oldVal, newVal) -> {
            if (progressTimeline != null)
                progressTimeline.stop();

            statusLabel.setText(funnyTexts.get((int) (Math.random() * funnyTexts.size())));
            percentLabel.setText(progressBar.getBar().getProgress() * 100 + " %");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox textContainer = new HBox(percentLabel, spacer, statusLabel);
        textContainer.setMaxWidth(480);

        VBox layout = new VBox(15, splashImage, textContainer, progressBar);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #1a1a1a; -fx-padding: 10;");

        Scene scene = new Scene(layout);
        splashStage.setScene(scene);
    }

    public void show(Consumer<ProgressBar> runnable) {
        splashStage.show();

        executorService.submit(() -> {
            runnable.accept(progressBar.getBar());

            Platform.runLater(() -> {
                splashStage.close();

                showMainWindow();
            });
        });
    }

    public abstract void showMainWindow();
    public abstract void onClose();
}
