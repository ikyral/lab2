package io.github.ikyral.lab2.maze.gui;

import io.github.ikyral.lab2.maze.MazeLogic;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Point;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainApp extends Application {

    private AppConfig config = new AppConfig();
    private final MazeLogic logic = new MazeLogic();

    private MazeCanvas mazeCanvas;
    private QValuesCanvas qValuesCanvas;

    private TextArea logArea;
    private ProgressBar progressBar;

    private Button btnConfig;
    private Button btnTrain;
    private Button btnReplay;
    private Slider speedSlider;
    private Spinner<Integer> spinEpisodes;
    private Label lblTrained;

    private final AtomicInteger pendingEpisodes = new AtomicInteger();
    private final AtomicInteger runQueued = new AtomicInteger();
    private final AtomicInteger runDone = new AtomicInteger();
    private final AtomicBoolean trainingRunning = new AtomicBoolean();

    private List<Point> pathForReplay;
    private int currentStepIndex = 0;
    private long lastStepTime = 0;
    private AnimationTimer replayTimer;
    private double replayEnergy = 0.0;

    @Override
    public void start(Stage primaryStage) {
        ConfigDialog dialog = new ConfigDialog(config);
        Optional<AppConfig> result = dialog.showAndWait();
        if (result.isPresent()) {
            config = result.get();
        } else {
            Platform.exit();
            return;
        }

        primaryStage.setTitle("Мышь в лабиринте — Обучение с подкреплением (Q-Learning)");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #12101B;");

        HBox topToolbar = createTopToolbar();
        root.setTop(topToolbar);

        mazeCanvas = new MazeCanvas(480, 480);
        qValuesCanvas = new QValuesCanvas(480, 480);

        VBox leftBox = new VBox(8, new Label("Карта среды и мышь:"), wrapResizable(mazeCanvas, mazeCanvas::redraw));
        VBox rightBox = new VBox(8, new Label("Веса Q-Learning (Q-Table & Направления):"),
                wrapResizable(qValuesCanvas, qValuesCanvas::redraw));
        setLabelsHeaderStyle(leftBox, rightBox);

        for (VBox box : new VBox[]{leftBox, rightBox}) {
            box.setPrefWidth(480);
            box.setMinWidth(0);
            HBox.setHgrow(box, Priority.ALWAYS);
        }

        HBox centerCanvases = new HBox(20, leftBox, rightBox);
        centerCanvases.setAlignment(Pos.CENTER);
        centerCanvases.setPadding(new Insets(10));
        root.setCenter(centerCanvases);

        VBox bottomBox = createBottomBar();
        root.setBottom(bottomBox);

        generateMaze();

        Scene scene = new Scene(root, 1100, 680);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    private Pane wrapResizable(Canvas canvas, Runnable redraw) {
        Pane holder = new Pane(canvas);
        holder.setMinSize(0, 0);
        holder.setPrefSize(480, 480);
        canvas.widthProperty().bind(holder.widthProperty());
        canvas.heightProperty().bind(holder.heightProperty());
        canvas.widthProperty().addListener((obs, oldV, newV) -> redraw.run());
        canvas.heightProperty().addListener((obs, oldV, newV) -> redraw.run());
        VBox.setVgrow(holder, Priority.ALWAYS);
        return holder;
    }

    private HBox createTopToolbar() {
        HBox bar = new HBox(15);
        bar.setPadding(new Insets(10, 15, 10, 15));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #1F1B30;");

        btnConfig = new Button("⚙ Изменить параметры");
        btnConfig.setOnAction(e -> openConfigDialog());

        Label lblEpisodes = new Label("Эпох:");
        lblEpisodes.setStyle("-fx-text-fill: #CCCCCC;");
        spinEpisodes = new Spinner<>(1, 1_000_000, 500, 100);
        spinEpisodes.setEditable(true);
        spinEpisodes.setPrefWidth(105);

        btnTrain = new Button("▶ Добавить эпохи");
        btnTrain.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;");
        btnTrain.setOnAction(e -> addEpisodes());

        btnReplay = new Button("🐭 Пройти оптимальный путь");
        btnReplay.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold;");
        btnReplay.setDisable(true);
        btnReplay.setOnAction(e -> replayBestPath());

        Label lblSpeed = new Label("Скорость анимации:");
        lblSpeed.setStyle("-fx-text-fill: #CCCCCC;");
        speedSlider = new Slider(1, 20, 6);
        speedSlider.setPrefWidth(120);

        bar.getChildren().addAll(btnConfig, new Separator(), lblEpisodes, spinEpisodes, btnTrain, btnReplay,
                new Separator(), lblSpeed, speedSlider);
        return bar;
    }

    private VBox createBottomBar() {
        VBox bottom = new VBox(6);
        bottom.setPadding(new Insets(10));
        bottom.setStyle("-fx-background-color: #151322;");

        lblTrained = new Label("Обучено эпох: 0");
        lblTrained.setStyle("-fx-text-fill: #E5E0FF; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        logArea = new TextArea();
        logArea.setPrefRowCount(3);
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background: #0E0D14; -fx-text-fill: #98FF98; -fx-font-family: monospace;");

        bottom.getChildren().addAll(lblTrained, progressBar, logArea);
        return bottom;
    }

    private void openConfigDialog() {
        stopReplay();
        ConfigDialog dialog = new ConfigDialog(config);
        Optional<AppConfig> result = dialog.showAndWait();
        if (result.isPresent()) {
            config = result.get();
            generateMaze();
        }
    }

    private void generateMaze() {
        stopReplay();
        logic.generateMaze(config);

        mazeCanvas.setGrid(logic.getGrid());
        mazeCanvas.setMousePosition(logic.getStartPos().x, logic.getStartPos().y);
        qValuesCanvas.updateData(logic.getGrid(), logic.getQTable());

        btnReplay.setDisable(true);
        progressBar.setProgress(0);
        updateTrainedLabel();
        log("Лабиринт сгенерирован: " + logic.getGrid().length + "x" + logic.getGrid()[0].length
                + " (Сыр: +" + config.rewardZ + ", Вода: +" + config.rewardX + ", Ток: -" + config.rewardY + ")");
    }

    private void addEpisodes() {
        try {
            spinEpisodes.increment(0);
        } catch (Exception ignored) {
        }
        Integer value = spinEpisodes.getValue();
        if (value == null || value <= 0) {
            log("Укажите положительное число эпох.");
            return;
        }
        final int n = value;

        if (trainingRunning.compareAndSet(false, true)) {
            stopReplay();
            setUiLocked(true);
            runQueued.set(n);
            runDone.set(0);
            pendingEpisodes.addAndGet(n);
            progressBar.setProgress(0);
            log("Обучение: +" + n + " эпох (α=" + config.alpha + ", γ=" + config.gamma + ").");
            startTrainingThread();
        } else {
            runQueued.addAndGet(n);
            pendingEpisodes.addAndGet(n);
            log("Добавлено в очередь: +" + n + " эпох.");
            updateTrainedLabel();
        }
    }

    private void startTrainingThread() {
        final AppConfig cfg = config;
        Thread worker = new Thread(() -> {
            try {
                do {
                    while (pendingEpisodes.getAndUpdate(p -> p > 0 ? p - 1 : 0) > 0) {
                        logic.trainSingleEpisode(cfg);
                        int done = runDone.incrementAndGet();

                        if (done % 25 == 0 || pendingEpisodes.get() == 0) {
                            final double progress = (double) done / Math.max(1, runQueued.get());
                            Platform.runLater(() -> {
                                progressBar.setProgress(Math.min(1.0, progress));
                                qValuesCanvas.updateData(logic.getGrid(), logic.getQTable());
                                updateTrainedLabel();
                            });
                        }
                    }

                    trainingRunning.set(false);
                } while (pendingEpisodes.get() != 0 && trainingRunning.compareAndSet(false, true));

                Platform.runLater(() -> {
                    qValuesCanvas.updateData(logic.getGrid(), logic.getQTable());
                    updateTrainedLabel();
                    if (!trainingRunning.get()) {
                        log("Обучение завершено. Всего эпох: " + logic.getEpisodeCount() + ". Карта весов обновлена!");
                        setUiLocked(false);
                        btnReplay.setDisable(logic.getEpisodeCount() == 0);
                    }
                });

            } catch (Exception ex) {
                pendingEpisodes.set(0);
                trainingRunning.set(false);
                Platform.runLater(() -> {
                    log("Ошибка обучения: " + ex.getMessage());
                    setUiLocked(false);
                    btnReplay.setDisable(logic.getEpisodeCount() == 0);
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void updateTrainedLabel() {
        int pending = pendingEpisodes.get();
        lblTrained.setText("Обучено эпох: " + logic.getEpisodeCount()
                + (pending > 0 ? "   (в очереди: " + pending + ")" : ""));
    }

    private void replayBestPath() {
        stopReplay();

        logic.resetGridToInitial();
        mazeCanvas.setGrid(logic.getGrid());

        pathForReplay = logic.getBestPath();

        if (pathForReplay == null || pathForReplay.isEmpty()) {
            log("Путь не найден.");
            return;
        }

        log("Анимация оптимального пути. Длина маршрута: " + (pathForReplay.size() - 1) + " шагов.");
        currentStepIndex = 0;
        lastStepTime = 0;
        replayEnergy = 0.0;

        replayTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double delayNano = 1_000_000_000.0 / speedSlider.getValue();
                if (now - lastStepTime >= delayNano) {
                    if (currentStepIndex < pathForReplay.size()) {
                        Point p = pathForReplay.get(currentStepIndex);

                        CellType cell = logic.getGrid()[p.x][p.y];

                        boolean drankWater = logic.consumeWater(p.x, p.y);
                        if (drankWater) {
                            replayEnergy += config.rewardX;
                            log("Мышь выпила воду на шаге " + currentStepIndex + "! Энергия: +"
                                    + config.rewardX + " (итого: " + replayEnergy + ")");
                        } else if (cell == CellType.SHOCK) {
                            replayEnergy -= config.rewardY;
                            log("Мышь наступила на ток на шаге " + currentStepIndex + "! Энергия: -"
                                    + config.rewardY + " (итого: " + replayEnergy + ")");
                        } else if (cell == CellType.CHEESE) {
                            replayEnergy += config.rewardZ;
                            log("Мышь нашла сыр на шаге " + currentStepIndex + "! Энергия: +"
                                    + config.rewardZ + " (итого: " + replayEnergy + ")");
                        }

                        mazeCanvas.setMousePosition(p.x, p.y);

                        currentStepIndex++;
                        lastStepTime = now;
                    } else {
                        stop();
                        Point lastPoint = pathForReplay.getLast();
                        if (lastPoint.equals(logic.getCheesePos())) {
                            log("Мышь успешно достигла сыра!");
                        } else {
                            log("Мышь застряла в цикле и не дошла до финиша.");
                        }
                        log("Итоговый счёт по энергии: " + replayEnergy);
                    }
                }
            }
        };
        replayTimer.start();
    }

    private void stopReplay() {
        if (replayTimer != null) {
            replayTimer.stop();
        }
    }

    private void setUiLocked(boolean locked) {
        btnConfig.setDisable(locked);
        btnReplay.setDisable(locked);
    }

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }

    private void setLabelsHeaderStyle(VBox... boxes) {
        for (VBox b : boxes) {
            if (!b.getChildren().isEmpty() && b.getChildren().getFirst() instanceof Label lbl) {
                lbl.setStyle("-fx-text-fill: #E5E0FF; -fx-font-weight: bold; -fx-font-size: 13px;");
            }
        }
    }
}