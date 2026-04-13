package rf.ebanina.UI.UI.Element;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import rf.ebanina.UI.Editors.IViewable;
import rf.ebanina.UI.UI.Element.ListViews.ListView;

import java.io.IOException;

public class MainFunctionDialog
        extends AnimationDialog {

    protected final ListView<IViewable> leftListView = new ListView<>();
    protected final StackPane leftPane = new StackPane();
    protected final StackPane rightPane = new StackPane();

    protected final VBox rightLayout = new VBox();
    protected final VBox topSpacer = new VBox();

    protected final VBox modsPanel = new VBox();
    protected final ScrollPane scrollPane = new ScrollPane();

    public ListView<IViewable> getLeftListView() {
        return leftListView;
    }

    public MainFunctionDialog(Stage ownerStage, Pane root) {
        super(ownerStage, root);

        setupModLayout();
        setupLeftList();
    }

    private void setupLeftList() {
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
            if (newItem != null) {
                try {
                    Parent content = newItem.parent();

                    rightPane.getChildren().clear();

                    if (content != null) {
                        rightPane.getChildren().add(content);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                rightPane.getChildren().clear();
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

    private void setupModLayout() {
        this.dialogBox.setPadding(Insets.EMPTY);

        GridPane grid = new GridPane();
        ColumnConstraints colLeft = new ColumnConstraints();
        colLeft.setPercentWidth(35);
        ColumnConstraints colRight = new ColumnConstraints();
        colRight.setPercentWidth(65);
        grid.getColumnConstraints().addAll(colLeft, colRight);

        RowConstraints row = new RowConstraints();
        row.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().add(row);

        grid.add(leftPane, 0, 0);
        grid.add(rightPane, 1, 0);

        rightLayout.setPadding(new Insets(20));
        rightLayout.setSpacing(0);

        rightPane.setBackground(new Background(new BackgroundFill(
                Color.web("#1E1E1E"),
                new CornerRadii(0, 16, 16, 0, false),  // 0, 16, 16, 0 — только правая сторона
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

        VBox.setVgrow(grid, Priority.ALWAYS);
        applyStyles();

        this.dialogBox.getChildren().add(grid);
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
}