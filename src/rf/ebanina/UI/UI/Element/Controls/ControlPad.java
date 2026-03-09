package rf.ebanina.UI.UI.Element.Controls;

import javafx.animation.AnimationTimer;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.function.BiConsumer;

public class ControlPad extends StackPane {
    private Pane arena;
    private int gridSquares = 20;
    private double arenaSize = 400;

    public Circle draggableCircle;

    // Физика движения и взаимодействия
    private double posX, posY;       // текущая позиция круга
    private double velX = 0, velY = 0; // скорость по X и Y
    private double targetX, targetY; // позиция курсора (цель)

    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;

    // Параметры физики
    private static final double SPRING_STRENGTH = 0.17;
    private static final double DAMPING = 0.6;
    private static final double MAX_SPEED = 2500;

    // Параметры для поворота (наклона круга)
    private static final double ROTATION_FACTOR = 0.5; // масштаб угла поворота
    private static final double ROTATION_DAMPING = 0.25; // затухание угла

    private double currentAngle = 0; // текущий угол поворота
    private double angularVelocity = 0; // скорость изменения угла

    private int FPS = 60;
    private int angleLimit = 15;
    private double angularVelocityCoefficient = 0.3;

    private double circleRadius;

    private CellFactory cellFactory = (row, col) -> {
        double cellSize = arenaSize / gridSquares;
        Region cell = new Region();
        cell.setPrefSize(cellSize, cellSize);
        cell.setLayoutX(col * cellSize);
        cell.setLayoutY(row * cellSize);
        cell.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        cell.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(0.5))));

        final int r = row;
        final int c = col;
        cell.setOnMouseClicked(e -> {
            double newX = c * cellSize + cellSize / 2;
            double newY = r * cellSize + cellSize / 2;
            setX(newX);
            setY(newY);
        });

        return cell;
    };

    public void setCellFactory(CellFactory factory) {
        this.cellFactory = factory != null ? factory : cellFactory;
        drawGrid();
    }

    public double getCircleRadius() {
        return circleRadius;
    }

    public ControlPad setCircleRadius(double circleRadius) {
        this.circleRadius = circleRadius;
        return this;
    }

    public CellFactory getCellFactory() {
        return cellFactory;
    }

    public int getFPS() {
        return FPS;
    }

    public ControlPad setFPS(int FPS) {
        this.FPS = FPS;
        return this;
    }

    public int getAngleLimit() {
        return angleLimit;
    }

    public ControlPad setAngleLimit(int angleLimit) {
        this.angleLimit = angleLimit;
        return this;
    }

    public double getAngularVelocityCoefficient() {
        return angularVelocityCoefficient;
    }

    public ControlPad setAngularVelocityCoefficient(double angularVelocityCoefficient) {
        this.angularVelocityCoefficient = angularVelocityCoefficient;
        return this;
    }

    public ControlPad(int gridSquares, double arenaSize, double circleRadius) {
        this.gridSquares = gridSquares;
        this.arenaSize = arenaSize;
        this.circleRadius = circleRadius;

        arena = new Pane();
        arena.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(5), null)));
        arena.setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1))));
        getChildren().add(arena);
        setPrefSize(arenaSize, arenaSize);
        arena.setPrefSize(arenaSize, arenaSize);
        drawGrid();

        draggableCircle = new Circle(circleRadius, Color.BLACK);
        draggableCircle.setCursor(Cursor.HAND);

        // Начальная позиция круга — центр арены
        posX = arenaSize / 2;
        posY = arenaSize / 2;
        draggableCircle.setCenterX(posX);
        draggableCircle.setCenterY(posY);

        arena.getChildren().add(draggableCircle);

        // Изначально цель равна текущей позиции круга
        targetX = posX;
        targetY = posY;

        // Обработчики мыши — управление целью движения круга
        draggableCircle.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                dragging = true;
                // Считаем сдвиг курсора относительно круга чтобы не «прыгать»
                dragOffsetX = event.getSceneX() - draggableCircle.localToScene(draggableCircle.getCenterX(), draggableCircle.getCenterY()).getX();
                dragOffsetY = event.getSceneY() - draggableCircle.localToScene(draggableCircle.getCenterX(), draggableCircle.getCenterY()).getY();
                // Обнуляем скорость и угловую скорость при новом захвате
                velX = 0;
                velY = 0;
                angularVelocity = 0;
                event.consume();
            }
        });

        draggableCircle.setOnMouseDragged(event -> {
            if (!dragging)
                return;

            // Новая цель — там где cursor с учётом сдвига
            double sceneX = event.getSceneX() - dragOffsetX;
            double sceneY = event.getSceneY() - dragOffsetY;

            // Конвертируем координаты сцены в локальные арены
            double localX = arena.sceneToLocal(sceneX, sceneY).getX();
            double localY = arena.sceneToLocal(sceneX, sceneY).getY();

            // Ограничиваем цель движением внутри арены с учётом радиуса круга
            targetX = Math.min(Math.max(localX, circleRadius), arenaSize - circleRadius);
            targetY = Math.min(Math.max(localY, circleRadius), arenaSize - circleRadius);

            event.consume();
        });

        draggableCircle.setOnMouseReleased(event -> {
            dragging = false;
            event.consume();
        });

        AnimationTimer timer = new AnimationTimer() {
            private long lastTime = -1;

            @Override
            public void handle(long now) {
                if (lastTime < 0) {
                    lastTime = now;
                    return;
                }

                double deltaTime = (now - lastTime) / 1_000_000_000.0; // в секундах
                lastTime = now;

                // Силы пружины (ускорение) к цели
                double forceX = (targetX - posX) * SPRING_STRENGTH;
                double forceY = (targetY - posY) * SPRING_STRENGTH;

                // Обновление скорости с учётом силы и времени (ускорение)
                velX += forceX * deltaTime * FPS; // домножили на 60 для примерно 60fps
                velY += forceY * deltaTime * FPS;

                // Демпфирование скорости (трение)
                velX *= DAMPING;
                velY *= DAMPING;

                // Ограничиваем скорость максимумом
                velX = Math.max(Math.min(velX, MAX_SPEED), -MAX_SPEED);
                velY = Math.max(Math.min(velY, MAX_SPEED), -MAX_SPEED);

                // Обновление позиции круга с учётом скорости и времени
                posX += velX * deltaTime * FPS;
                posY += velY * deltaTime * FPS;

                // Ограничение позиции круга внутри арены
                posX = Math.min(Math.max(posX, circleRadius), arenaSize - circleRadius);
                posY = Math.min(Math.max(posY, circleRadius), arenaSize - circleRadius);

                draggableCircle.setCenterX(posX);
                draggableCircle.setCenterY(posY);

                // --- Поворот (вращение) для эффекта наклона ---
                double targetAngle = -velX * ROTATION_FACTOR;

                angularVelocity += (targetAngle - currentAngle) * angularVelocityCoefficient;
                angularVelocity *= ROTATION_DAMPING;

                currentAngle += angularVelocity;

                currentAngle = Math.max(Math.min(currentAngle, angleLimit), -angleLimit);

                draggableCircle.setRotate(currentAngle);
            }
        };

        draggableCircle.setOnMouseDragged(event -> {
            if (!dragging)
                return;

            double sceneX = event.getSceneX() - dragOffsetX;
            double sceneY = event.getSceneY() - dragOffsetY;

            double localX = arena.sceneToLocal(sceneX, sceneY).getX();
            double localY = arena.sceneToLocal(sceneX, sceneY).getY();

            targetX = Math.min(Math.max(localX, circleRadius), arenaSize - circleRadius);
            targetY = Math.min(Math.max(localY, circleRadius), arenaSize - circleRadius);

            double normX = getNormalizedX();
            double normY = getNormalizedY();

            onDragCircle.accept(normX, normY);

            event.consume();
        });

        timer.start();
    }

    public ControlPad(int gridSquares, double arenaSize) {
        this(gridSquares, arenaSize, 10);
    }

    public ControlPad() {
        this(20, 400);
    }

    public BiConsumer<Double, Double> onDragCircle = (aFloat, aFloat2) -> {};

    private BiConsumer<Integer, Integer> onCellClicked = (row, col) -> {
        double cellSize = arenaSize / gridSquares;
        double newX = col * cellSize + cellSize / 2;
        double newY = row * cellSize + cellSize / 2;

        setX(newX);
        setY(newY);
    };


    private void drawGrid() {
        arena.getChildren().clear();
        for (int row = 0; row < gridSquares; row++) {
            for (int col = 0; col < gridSquares; col++) {
                Region cell = cellFactory.createCell(row, col);
                arena.getChildren().add(cell);
            }
        }
    }

    public void setOnCellClicked(BiConsumer<Integer, Integer> handler) {
        this.onCellClicked = handler != null ? handler : (r, c) -> {};
        drawGrid();
    }

    public void setGridSquares(int gridSquares) {
        this.gridSquares = gridSquares > 0 ? gridSquares : 1;
        drawGrid();
    }

    public int getGridSquares() {
        return gridSquares;
    }

    /**
     * Получить нормализованное положение круга по X в диапазоне [-1, 1].
     */
    public double getNormalizedX() {
        double halfArena = arenaSize / 2;
        return (posX - halfArena) / (halfArena - circleRadius);
    }

    /**
     * Получить нормализованное положение круга по Y в диапазоне [-1, 1].
     */
    public double getNormalizedY() {
        double halfArena = arenaSize / 2;
        return (posY - halfArena) / (halfArena - circleRadius);
    }

    public void setArenaSize(double size) {
        this.arenaSize = size > 0 ? size : 100;
        setPrefSize(arenaSize, arenaSize);
        arena.setPrefSize(arenaSize, arenaSize);
        drawGrid();

        // Обновляем позицию и цель круга пропорционально новому размеру
        posX = arenaSize / 2;
        posY = arenaSize / 2;
        targetX = posX;
        targetY = posY;
        draggableCircle.setCenterX(posX);
        draggableCircle.setCenterY(posY);
    }

    /**
     * Установить позицию круга по X с ограничением внутри арены и обновлением цели движения.
     */
    public void setX(double x) {
        // Ограничиваем x с учетом радиуса круга и размера арены
        posX = Math.min(Math.max(x, circleRadius), arenaSize - circleRadius);
        targetX = posX;
        draggableCircle.setCenterX(posX);
    }

    /**
     * Установить позицию круга по Y с ограничением внутри арены и обновлением цели движения.
     */
    public void setY(double y) {
        // Ограничиваем y с учетом радиуса круга и размера арены
        posY = Math.min(Math.max(y, circleRadius), arenaSize - circleRadius);
        targetY = posY;
        draggableCircle.setCenterY(posY);
    }

    public double getArenaSize() {
        return arenaSize;
    }

    public Pane getArena() {
        return arena;
    }
}
