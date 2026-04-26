package rf.ebanina.UI.UI.Element.Dialogs;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
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
        extends AnimationDialog {
    protected ListView<IViewable> leftListView = new ListView<>();

    protected StackPane leftPane = new StackPane();
    protected StackPane rightPane = new StackPane();

    protected VBox rightLayout = new VBox();
    protected VBox topSpacer = new VBox();

    protected VBox modsPanel = new VBox();

    protected GridPane mainSetupGrid;
    protected ColumnConstraints leftColumn;
    protected ColumnConstraints rightColumn;

    protected ScrollPane scrollPane = new ScrollPane();

    private static final Duration DURATION = Duration.millis(1000);

    private ParallelTransition currentOutTransition;
    private ParallelTransition currentInTransition;
    private int lastSelectedIndex = -1;

    public MainFunctionDialog(Stage ownerStage, Pane root) {
        super(ownerStage, root);

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
            if (newItem == null) return;

            rightPane.getChildren().clear();

            if (currentOutTransition != null)
                currentOutTransition.stop();
            if (currentInTransition != null)
                currentInTransition.stop();

            try {
                Parent newContent = newItem.parent();
                if (newContent == null)
                    return;

                int newIndex = leftListView.getSelectionModel().getSelectedIndex();
                double height = rightPane.getHeight();
                boolean movingDown = newIndex > lastSelectedIndex;
                lastSelectedIndex = newIndex;

                if (oldItem != null) {
                    Parent oldContent = oldItem.parent();
                    rightPane.getChildren().add(oldContent);

                    TranslateTransition out = new TranslateTransition(DURATION, oldContent);
                    out.setInterpolator(Root.iceInterpolator);
                    out.setToY(movingDown ? -height : height);
                    FadeTransition fadeOut = new FadeTransition(DURATION, oldContent);
                    fadeOut.setToValue(0);

                    currentOutTransition = new ParallelTransition(out, fadeOut);
                    currentOutTransition.setOnFinished(e -> rightPane.getChildren().remove(oldContent));
                    currentOutTransition.play();
                }

                newContent.setTranslateY(movingDown ? height : -height);
                newContent.setOpacity(0);
                rightPane.getChildren().add(newContent);

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

    private void setupRight() {
        this.dialogBox.setPadding(Insets.EMPTY);

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

        rightLayout.setPadding(new Insets(20));
        rightLayout.setSpacing(0);

        rightPane.setBackground(new Background(new BackgroundFill(
                Color.web("#1E1E1E"),
                new CornerRadii(0, 16, 16, 0, false),
                Insets.EMPTY
        )));

        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-viewport-background-color: transparent;");

        scrollPane.getStylesheets().add("data:text/css," +
                ".scroll-bar:vertical { -fx-background-color: transparent; -fx-width: 6; }" +
                ".scroll-bar:vertical .thumb { -fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 10; }" +
                ".scroll-bar:vertical .track, .scroll-bar:vertical .increment-button, .scroll-bar:vertical .decrement-button { -fx-background-color: transparent; -fx-opacity: 0; }");

        modsPanel.setPrefHeight(0);
        modsPanel.setMinHeight(0);
        modsPanel.setOpacity(0);
        modsPanel.getChildren().add(scrollPane);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rightLayout.getChildren().addAll(topSpacer, modsPanel);
        rightPane.getChildren().add(rightLayout);

        rightPane.setPadding(new Insets(15, 15, 15, 15));

        VBox.setVgrow(mainSetupGrid, Priority.ALWAYS);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);

        clip.widthProperty().bind(rightPane.widthProperty());
        clip.heightProperty().bind(rightPane.heightProperty());

        rightPane.setClip(clip);

        applyStyles();

        this.dialogBox.getChildren().add(mainSetupGrid);
    }

    private void applyStyles() {
        leftPane.setBackground(new Background(new BackgroundFill(
                Color.web("#181818"), new CornerRadii(16, 0, 0, 16, false), Insets.EMPTY)));
        leftPane.setPadding(new Insets(20));
        leftPane.setBorder(new Border(new BorderStroke(
                Color.gray(0.2), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0, 1, 0, 0))));

        rightPane.setBackground(new Background(new BackgroundFill(
                Color.web("#1E1E1E"), new CornerRadii(0, 16, 16, 0, false), Insets.EMPTY)));

        modsPanel.setBackground(new Background(new BackgroundFill(
                Color.web("#141414"), new CornerRadii(0, 0, 16, 16, false), Insets.EMPTY)));
        modsPanel.setBorder(new Border(new BorderStroke(
                Color.gray(0.3), BorderStrokeStyle.SOLID, new CornerRadii(0, 0, 16, 16, false), new BorderWidths(0, 1, 1, 1))));
    }

    public ListView<IViewable> getLeftListView() {
        return leftListView;
    }

    public MainFunctionDialog setLeftListView(ListView<IViewable> leftListView) {
        this.leftListView = leftListView;
        return this;
    }

    public StackPane getLeftPane() {
        return leftPane;
    }

    public MainFunctionDialog setLeftPane(StackPane leftPane) {
        this.leftPane = leftPane;
        return this;
    }

    public StackPane getRightPane() {
        return rightPane;
    }

    public MainFunctionDialog setRightPane(StackPane rightPane) {
        this.rightPane = rightPane;
        return this;
    }

    public VBox getRightLayout() {
        return rightLayout;
    }

    public MainFunctionDialog setRightLayout(VBox rightLayout) {
        this.rightLayout = rightLayout;
        return this;
    }

    public VBox getTopSpacer() {
        return topSpacer;
    }

    public MainFunctionDialog setTopSpacer(VBox topSpacer) {
        this.topSpacer = topSpacer;
        return this;
    }

    public VBox getModsPanel() {
        return modsPanel;
    }

    public MainFunctionDialog setModsPanel(VBox modsPanel) {
        this.modsPanel = modsPanel;
        return this;
    }

    public GridPane getMainSetupGrid() {
        return mainSetupGrid;
    }

    public MainFunctionDialog setMainSetupGrid(GridPane mainSetupGrid) {
        this.mainSetupGrid = mainSetupGrid;
        return this;
    }

    public ColumnConstraints getLeftColumn() {
        return leftColumn;
    }

    public MainFunctionDialog setLeftColumn(ColumnConstraints leftColumn) {
        this.leftColumn = leftColumn;
        return this;
    }

    public ColumnConstraints getRightColumn() {
        return rightColumn;
    }

    public MainFunctionDialog setRightColumn(ColumnConstraints rightColumn) {
        this.rightColumn = rightColumn;
        return this;
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    public MainFunctionDialog setScrollPane(ScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        return this;
    }
}