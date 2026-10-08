package io.github.ikyral.lab2.maze.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;


public class QValuesCanvas extends Canvas {

    private double[][][] qTable;
    private CellType[][] grid;

    public QValuesCanvas(double width, double height) {
        super(width, height);
    }

    public void updateData(CellType[][] grid, double[][][] qTable) {
        this.grid = grid;
        this.qTable = qTable;
        redraw();
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.setFill(Color.web("#1F1F22"));
        gc.fillRect(0, 0, w, h);

        if (grid == null || grid.length == 0 || grid[0].length == 0) return;

        int rows = grid.length;
        int cols = grid[0].length;
        double cellSize = Math.min(w / cols, h / rows);
        double offsetX = (w - cellSize * cols) / 2.0;
        double offsetY = (h - cellSize * rows) / 2.0;

        double maxAbsQ = 1.0;
        if (qTable != null) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    for (int a = 0; a < 4; a++) {
                        maxAbsQ = Math.max(maxAbsQ, Math.abs(qTable[r][c][a]));
                    }
                }
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = offsetX + c * cellSize;
                double y = offsetY + r * cellSize;

                if (grid[r][c] == CellType.WALL) {
                    gc.setFill(Color.web("#50535A"));
                    gc.fillRect(x, y, cellSize, cellSize);
                    continue;
                }

                double bestQ = 0.0;
                int bestAction = 0;

                if (qTable != null) {
                    bestQ = qTable[r][c][0];
                    for (int a = 1; a < 4; a++) {
                        if (qTable[r][c][a] > bestQ) {
                            bestQ = qTable[r][c][a];
                            bestAction = a;
                        }
                    }
                }

                Color cellColor = computeQColor(bestQ, maxAbsQ);
                gc.setFill(cellColor);
                gc.fillRect(x, y, cellSize, cellSize);
                gc.setStroke(Color.web("#383A42"));
                gc.setLineWidth(1.0);
                gc.strokeRect(x, y, cellSize, cellSize);

                if (qTable != null && Math.abs(bestQ) > 0.001) {
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.setFill(Color.WHITE);
                    gc.setFont(Font.font("System", FontWeight.BOLD, cellSize * 0.35));

                    String arrow = switch (bestAction) {
                        case 0 -> "↑";
                        case 1 -> "↓";
                        case 2 -> "←";
                        case 3 -> "→";
                        default -> "";
                    };
                    gc.fillText(arrow, x + cellSize / 2, y + cellSize * 0.45);

                    gc.setFont(Font.font("Monospaced", cellSize * 0.22));
                    gc.fillText(String.format("%.1f", bestQ), x + cellSize / 2, y + cellSize * 0.85);
                }
            }
        }
    }

    private Color computeQColor(double q, double maxAbs) {
        if (Math.abs(q) < 0.001) return Color.web("#25262B");

        double norm = Math.min(1.0, Math.abs(q) / maxAbs);
        if (q > 0) {
            return Color.color(0.12, 0.25 + 0.55 * norm, 0.18, 0.95);
        } else {
            return Color.color(0.35 + 0.55 * norm, 0.12, 0.12, 0.95);
        }
    }
}