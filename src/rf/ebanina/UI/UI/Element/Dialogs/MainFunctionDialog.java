package rf.ebanina.UI.UI.Element.Dialogs;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import rf.ebanina.UI.Editors.IViewable;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.ListViews.ListView;

import java.io.IOException;

public class MainFunctionDialog
        extends AnimationDialog
{
    protected ListView<IViewable> leftListView = new ListView<>();

    private Rectangle clip;

    protected StackPane leftPane = new StackPane();
    protected StackPane rightPane = new StackPane();

    protected VBox rightOverlay = new VBox();
    protected StackPane contentPane = new StackPane();

    protected GridPane mainSetupGrid;
    protected ColumnConstraints leftColumn;
    protected ColumnConstraints rightColumn;

    private static final Duration DURATION = Duration.millis(1000);

    private ParallelTransition currentOutTransition;
    private ParallelTransition currentInTransition;
    private int lastSelectedIndex = -1;

    public MainFunctionDialog(Stage ownerStage, Pane root) {
        super(ownerStage, root);

        setupGrid();
        setupRightOverlay();
        setupRight();
        setupLeft();
    }

    private void setupLeft() {
        leftPane.getChildren().add(leftListView);
        VBox.setVgrow(leftListView, Priority.ALWAYS);

        leftListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<IViewable> call(javafx.scene.control.ListView<IViewable> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(IViewable item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText("");
                        } else {
                            setText(item.name());
                        }
                    }
                };
            }
        });

        leftListView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null)
                return;

            contentPane.getChildren().clear();

            if (currentOutTransition != null)
                currentOutTransition.stop();
            if (currentInTransition != null)
                currentInTransition.stop();

            try {
                Parent newContent = newItem.parent();
                if (newContent == null)
                    return;

                int newIndex = leftListView.getSelectionModel().getSelectedIndex();
                double height = contentPane.getHeight();
                boolean movingDown = newIndex > lastSelectedIndex;
                lastSelectedIndex = newIndex;

                if (oldItem != null) {
                    Parent oldContent = oldItem.parent();
                    contentPane.getChildren().add(oldContent);

                    TranslateTransition out = new TranslateTransition(DURATION, oldContent);
                    out.setInterpolator(Root.iceInterpolator);
                    out.setToY(movingDown ? -height : height);
                    FadeTransition fadeOut = new FadeTransition(DURATION, oldContent);
                    fadeOut.setToValue(0);

                    currentOutTransition = new ParallelTransition(out, fadeOut);
                    currentOutTransition.setOnFinished(e -> contentPane.getChildren().remove(oldContent));
                    currentOutTransition.play();
                }

                newContent.setTranslateY(movingDown ? height : -height);
                newContent.setOpacity(0);
                contentPane.getChildren().add(newContent);

                TranslateTransition in = new TranslateTransition(DURATION, newContent);
                in.setInterpolator(Root.iceInterpolator);
                in.setToY(0);
                FadeTransition fadeIn = new FadeTransition(DURATION, newContent);
                fadeIn.setToValue(1);

                currentInTransition = new ParallelTransition(in, fadeIn);
                currentInTransition.play();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        leftListView.getStylesheets().add("data:text/css," +
                ".list-view { " +
                "-fx-background-color: transparent; " +
                "-fx-border-width: 0; " +
                "-fx-padding: 4; " +
                "} " +
                ".list-view .virtual-flow { " +
                "-fx-background-color: transparent; " +
                "} " +
                ".list-view .list-cell { " +
                "-fx-text-fill: white; " +
                "-fx-background-color: transparent; " +
                "-fx-padding: 6 8; " +
                "} " +
                ".list-view .list-cell:selected { " +
                "-fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-text-fill: white; " +
                "}");

        leftListView.getStylesheets().add("data:text/css," +
                ".list-view .scroll-bar:vertical, " +
                ".list-view .scroll-bar:horizontal { " +
                "-fx-opacity: 0; " +
                "-fx-pref-width: 0; " +
                "-fx-max-width: 0; " +
                "-fx-pref-height: 0; " +
                "-fx-max-height: 0; " +
                "}");
    }

    private void setupGrid() {
        mainSetupGrid = new GridPane();

        leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(25);
        rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(75);
        mainSetupGrid.getColumnConstraints().addAll(leftColumn, rightColumn);

        RowConstraints row = new RowConstraints();
        row.setVgrow(Priority.ALWAYS);
        mainSetupGrid.getRowConstraints().add(row);

        mainSetupGrid.add(leftPane, 0, 0);
        mainSetupGrid.add(rightPane, 1, 0);
        mainSetupGrid.add(rightOverlay, 1, 0);

        dialogBox.getChildren().add(mainSetupGrid);

        VBox.setVgrow(mainSetupGrid, Priority.ALWAYS);
    }

    private void setupRightOverlay() {
        rightOverlay.setMouseTransparent(true);
    }

    private void setupRight() {
        this.dialogBox.setPadding(Insets.EMPTY);

        rightPane.setPadding(new Insets(0));

        rightPane.getChildren().add(contentPane);
        rightPane.getChildren().add(rightOverlay);

        contentPane.setPadding(new Insets(0));

        createClip();
        applyStyles();
    }

    private void createClip() {
        clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(rightPane.widthProperty());
        clip.heightProperty().bind(rightPane.heightProperty());

        rightPane.setClip(clip);
    }

    private void applyStyles() {
        leftPane.setBackground(new Background(new BackgroundFill(
                Color.web("#181818"), new CornerRadii(16, 0, 0, 16, false), Insets.EMPTY)));
        leftPane.setPadding(new Insets(20));
        leftPane.setBorder(new Border(new BorderStroke(
                Color.gray(0.2), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 1, 0, 0))));

        rightPane.setBackground(new Background(new BackgroundFill(
                Color.web("#1E1E1E"),
                new CornerRadii(0, 16, 16, 0, false),
                Insets.EMPTY
        )));
    }

    public ListView<IViewable> getLeftListView() {
        return leftListView;
    }

    public GridPane getMainSetupGrid() {
        return mainSetupGrid;
    }
}