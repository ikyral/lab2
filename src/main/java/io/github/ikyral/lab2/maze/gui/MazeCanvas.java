package io.github.ikyral.lab2.maze.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MazeCanvas extends Canvas {

    private CellType[][] grid;
    private int mouseRow = -1;
    private int mouseCol = -1;

    public MazeCanvas(double width, double height) {
        super(width, height);
    }

    public void setGrid(CellType[][] grid) {
        this.grid = grid;
        redraw();
    }

    public void setMousePosition(int row, int col) {
        this.mouseRow = row;
        this.mouseCol = col;
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

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = offsetX + c * cellSize;
                double y = offsetY + r * cellSize;

                if (grid[r][c] == CellType.WALL) {
                    gc.setFill(Color.web("#50535A"));
                    gc.fillRect(x, y, cellSize, cellSize);
                } else {
                    gc.setFill(Color.web("#2A2B30"));
                    gc.fillRect(x, y, cellSize, cellSize);
                    gc.setStroke(Color.web("#383A42"));
                    gc.setLineWidth(1.0);
                    gc.strokeRect(x, y, cellSize, cellSize);

                    switch (grid[r][c]) {
                        case CHEESE -> drawCheese(gc, x, y, cellSize);
                        case SHOCK  -> drawShock(gc, x, y, cellSize);
                        case WATER  -> drawWater(gc, x, y, cellSize);
                        default     -> {}
                    }
                }
            }
        }

        if (mouseRow >= 0 && mouseCol >= 0 && mouseRow < rows && mouseCol < cols) {
            double mx = offsetX + mouseCol * cellSize;
            double my = offsetY + mouseRow * cellSize;
            drawMouse(gc, mx, my, cellSize);
        }
    }

    private void drawCheese(GraphicsContext gc, double x, double y, double size) {
        double pad = size * 0.18;
        double w = size - 2 * pad;
        double h = size - 2 * pad;
        double sx = x + pad;
        double sy = y + pad;

        gc.setFill(Color.web("#FFC107"));
        gc.fillPolygon(
                new double[]{sx, sx + w, sx + w * 0.25},
                new double[]{sy + h, sy + h, sy},
                3
        );

        gc.setFill(Color.web("#D39E00"));
        gc.fillOval(sx + w * 0.35, sy + h * 0.65, w * 0.22, w * 0.22);
        gc.fillOval(sx + w * 0.65, sy + h * 0.55, w * 0.18, w * 0.18);
        gc.fillOval(sx + w * 0.30, sy + h * 0.35, w * 0.14, w * 0.14);
    }

    private void drawShock(GraphicsContext gc, double x, double y, double size) {
        double pad = size * 0.16;
        double sx = x + pad;
        double sy = y + pad;
        double s = size - 2 * pad;

        gc.setFill(Color.web("#FFEB3B"));
        gc.setStroke(Color.web("#FF9800"));
        gc.setLineWidth(1.2);

        double[] px = {
                sx + s * 0.55,
                sx + s * 0.25,
                sx + s * 0.48,
                sx + s * 0.32,
                sx + s * 0.78,
                sx + s * 0.52
        };
        double[] py = {
                sy + s * 0.05,
                sy + s * 0.50,
                sy + s * 0.50,
                sy + s * 0.95,
                sy + s * 0.45,
                sy + s * 0.45
        };

        gc.fillPolygon(px, py, 6);
        gc.strokePolygon(px, py, 6);
    }

    private void drawWater(GraphicsContext gc, double x, double y, double size) {
        double pad = size * 0.22;
        double sx = x + pad;
        double sy = y + pad;
        double s = size - 2 * pad;

        gc.setFill(Color.web("#00B0FF"));
        gc.fillOval(sx + s * 0.1, sy + s * 0.25, s * 0.8, s * 0.75);
        gc.fillPolygon(
                new double[]{sx + s * 0.15, sx + s * 0.5, sx + s * 0.85},
                new double[]{sy + s * 0.5, sy, sy + s * 0.5},
                3
        );
    }

    private void drawMouse(GraphicsContext gc, double x, double y, double size) {
        double pad = size * 0.15;
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;
        double r = (size - 2 * pad) / 2.0;

        gc.setFill(Color.WHITE);
        gc.fillOval(cx - r * 0.95, cy - r * 0.95, r * 0.8, r * 0.8);
        gc.fillOval(cx + r * 0.15, cy - r * 0.95, r * 0.8, r * 0.8);

        gc.setFill(Color.web("#FF80AB"));
        gc.fillOval(cx - r * 0.8, cy - r * 0.8, r * 0.5, r * 0.5);
        gc.fillOval(cx + r * 0.3, cy - r * 0.8, r * 0.5, r * 0.5);

        gc.setFill(Color.WHITE);
        gc.fillOval(cx - r * 0.75, cy - r * 0.65, r * 1.5, r * 1.4);

        gc.setFill(Color.BLACK);
        gc.fillOval(cx - r * 0.45, cy - r * 0.2, r * 0.22, r * 0.22);
        gc.fillOval(cx + r * 0.23, cy - r * 0.2, r * 0.22, r * 0.22);

        gc.setFill(Color.web("#FF4081"));
        gc.fillOval(cx - r * 0.15, cy + r * 0.25, r * 0.3, r * 0.22);
    }
}