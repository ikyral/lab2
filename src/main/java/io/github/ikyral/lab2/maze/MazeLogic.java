package io.github.ikyral.lab2.maze;

import io.github.ikyral.lab2.maze.gui.AppConfig;
import io.github.ikyral.lab2.maze.gui.CellType;

import java.awt.Point;
import java.util.*;

public class MazeLogic {

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private static final double EPSILON_DECAY_PER_EPISODE = 0.00044;

    private CellType[][] grid;
    private CellType[][] initialGrid;
    private Point startPos = new Point(1, 1);
    private Point cheesePos = new Point(1, 1);

    private double[][][] qBase;
    private double[][][][] qWater;
    private int[][] waterId;
    private int waterCount;
    private boolean[][][] tried;

    private AppConfig config = new AppConfig();
    private volatile int episodeCount = 0;
    private final Random random = new Random();

    public void generateMaze(AppConfig config) {
        int rows = config.heightRooms * 2 + 1;
        int cols = config.widthRooms * 2 + 1;

        grid = new CellType[rows][cols];
        episodeCount = 0;
        this.config = config;

        for (int r = 0; r < rows; r++) {
            Arrays.fill(grid[r], CellType.WALL);
        }

        boolean[][] visited = new boolean[rows][cols];
        Stack<Point> stack = new Stack<>();
        Point startRoom = new Point(1, 1);
        grid[startRoom.x][startRoom.y] = CellType.EMPTY;
        visited[startRoom.x][startRoom.y] = true;
        stack.push(startRoom);

        int[][] dfsSteps = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};

        while (!stack.isEmpty()) {
            Point current = stack.peek();
            List<Point> unvisitedNeighbors = new ArrayList<>();

            for (int[] step : dfsSteps) {
                int nr = current.x + step[0];
                int nc = current.y + step[1];

                if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && !visited[nr][nc]) {
                    unvisitedNeighbors.add(new Point(nr, nc));
                }
            }

            if (!unvisitedNeighbors.isEmpty()) {
                Point next = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                int wallR = current.x + (next.x - current.x) / 2;
                int wallC = current.y + (next.y - current.y) / 2;
                grid[wallR][wallC] = CellType.EMPTY;

                grid[next.x][next.y] = CellType.EMPTY;
                visited[next.x][next.y] = true;
                stack.push(next);
            } else {
                stack.pop();
            }
        }

        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (grid[r][c] == CellType.WALL && random.nextDouble() < 0.08) {
                    boolean verticalOpen = (grid[r - 1][c] == CellType.EMPTY && grid[r + 1][c] == CellType.EMPTY);
                    boolean horizontalOpen = (grid[r][c - 1] == CellType.EMPTY && grid[r][c + 1] == CellType.EMPTY);
                    if (verticalOpen || horizontalOpen) {
                        grid[r][c] = CellType.EMPTY;
                    }
                }
            }
        }

        startPos = new Point(1, 1);
        cheesePos = new Point(rows - 2, cols - 2);
        grid[startPos.x][startPos.y] = CellType.EMPTY;
        grid[cheesePos.x][cheesePos.y] = CellType.CHEESE;

        placeObjects(rows, cols, config);

        saveInitialGrid();
        initQTables(config);
    }

    private void placeObjects(int rows, int cols, AppConfig config) {
        List<Point> emptyCells = new ArrayList<>();
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                if (grid[r][c] == CellType.EMPTY
                        && !(r == startPos.x && c == startPos.y)
                        && !(r == cheesePos.x && c == cheesePos.y)) {
                    emptyCells.add(new Point(r, c));
                }
            }
        }

        Collections.shuffle(emptyCells, random);
        int availableCells = emptyCells.size();

        int waterCount;
        if (config.customWaterCount != null) {
            waterCount = Math.min(config.customWaterCount, availableCells);
        } else {
            waterCount = Math.max(2, availableCells / 10);
        }

        int shockCount;
        int remainingCells = Math.max(0, availableCells - waterCount);
        if (config.customShockCount != null) {
            shockCount = Math.min(config.customShockCount, remainingCells);
        } else {
            shockCount = Math.max(2, availableCells / 12);
            shockCount = Math.min(shockCount, remainingCells);
        }

        int idx = 0;
        for (int i = 0; i < waterCount && idx < emptyCells.size(); i++, idx++) {
            Point p = emptyCells.get(idx);
            grid[p.x][p.y] = CellType.WATER;
        }

        for (int i = 0; i < shockCount && idx < emptyCells.size(); i++, idx++) {
            Point p = emptyCells.get(idx);
            grid[p.x][p.y] = CellType.SHOCK;
        }
    }

    private void saveInitialGrid() {
        int rows = grid.length;
        int cols = grid[0].length;
        initialGrid = new CellType[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(grid[r], 0, initialGrid[r], 0, cols);
        }
    }

    public void resetGridToInitial() {
        if (initialGrid == null) return;
        for (int r = 0; r < grid.length; r++) {
            System.arraycopy(initialGrid[r], 0, grid[r], 0, grid[0].length);
        }
    }

    public boolean consumeWater(int r, int c) {
        if (grid != null && grid[r][c] == CellType.WATER) {
            grid[r][c] = CellType.EMPTY;
            return true;
        }
        return false;
    }

    private void initQTables(AppConfig cfg) {
        int rows = grid.length;
        int cols = grid[0].length;

        waterId = new int[rows][cols];
        waterCount = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                waterId[r][c] = (grid[r][c] == CellType.WATER) ? waterCount++ : -1;
            }
        }

        qBase = new double[rows][cols][4];
        for (double[][] row : qBase) {
            for (double[] cell : row) Arrays.fill(cell, cfg.rewardZ);
        }
        qWater = new double[waterCount][rows][cols][4];
        for (double[][][] table : qWater) {
            for (double[][] row : table) {
                for (double[] cell : row) Arrays.fill(cell, cfg.rewardX);
            }
        }
        tried = new boolean[rows][cols][4];
    }

    private boolean isFree(int r, int c) {
        return r >= 0 && r < grid.length && c >= 0 && c < grid[0].length && grid[r][c] != CellType.WALL;
    }

    private double totalQ(int r, int c, int a, boolean[] drunk) {
        double q = qBase[r][c][a];
        for (int w = 0; w < waterCount; w++) {
            if (!drunk[w]) q += qWater[w][r][c][a];
        }
        return q;
    }

    private int getBestAction(int r, int c, boolean[] drunk) {
        int[] best = new int[4];
        int count = 0;
        double maxQ = -Double.MAX_VALUE;

        for (int a = 0; a < 4; a++) {
            if (!isFree(r + DR[a], c + DC[a])) continue;

            double q = totalQ(r, c, a, drunk);
            if (q > maxQ + 1e-6) {
                maxQ = q;
                count = 0;
                best[count++] = a;
            } else if (Math.abs(q - maxQ) <= 1e-6) {
                best[count++] = a;
            }
        }
        return count == 0 ? 0 : best[random.nextInt(count)];
    }

    public void trainSingleEpisode(AppConfig cfg) {
        resetGridToInitial();
        config = cfg;
        episodeCount++;
        double epsilon = Math.max(0.08, 1.0 - episodeCount * EPSILON_DECAY_PER_EPISODE);

        int currR = startPos.x;
        int currC = startPos.y;

        int maxSteps = grid.length * grid[0].length * 4;
        boolean[] drunk = new boolean[waterCount];

        for (int step = 0; step < maxSteps; step++) {
            int action = (random.nextDouble() < epsilon)
                    ? random.nextInt(4)
                    : getBestAction(currR, currC, drunk);
            tried[currR][currC][action] = true;

            int nextR = currR + DR[action];
            int nextC = currC + DC[action];

            double baseReward;
            boolean terminal = false;
            int hitWater = -1;

            if (!isFree(nextR, nextC)) {
                baseReward = -cfg.rewardY;
                nextR = currR;
                nextC = currC;
            } else {
                switch (grid[nextR][nextC]) {
                    case CHEESE -> {
                        baseReward = cfg.rewardZ;
                        terminal = true;
                    }
                    case SHOCK -> baseReward = -cfg.rewardY;
                    case WATER -> {
                        baseReward = 0.0;
                        int id = waterId[nextR][nextC];
                        if (id >= 0 && !drunk[id]) {
                            hitWater = id;
                            drunk[id] = true;
                        }
                    }
                    default -> baseReward = 0.0;
                }
            }

            int nextAction = terminal ? 0 : getBestAction(nextR, nextC, drunk);

            double[] qb = qBase[currR][currC];
            double targetBase = baseReward + (terminal ? 0.0 : cfg.gamma * qBase[nextR][nextC][nextAction]);
            qb[action] += cfg.alpha * (targetBase - qb[action]);

            for (int w = 0; w < waterCount; w++) {
                if (drunk[w] && w != hitWater) continue;
                double[] qw = qWater[w][currR][currC];
                double target;
                if (w == hitWater) {
                    target = cfg.rewardX;
                } else {
                    target = terminal ? 0.0 : cfg.gamma * qWater[w][nextR][nextC][nextAction];
                }
                qw[action] += cfg.alpha * (target - qw[action]);
            }

            currR = nextR;
            currC = nextC;

            if (terminal) break;
        }
    }

    private int getPathAction(int r, int c, boolean[] drunk, int drunkCount, Set<Long> visited) {
        int cols = grid[0].length;
        int best = -1;
        boolean bestFresh = false;
        double bestQ = -Double.MAX_VALUE;

        for (int a = 0; a < 4; a++) {
            int nr = r + DR[a];
            int nc = c + DC[a];
            if (!isFree(nr, nc)) continue;

            int id = waterId[nr][nc];
            int newCount = drunkCount + ((id >= 0 && !drunk[id]) ? 1 : 0);
            boolean fresh = !visited.contains(((long) nr * cols + nc) * 100_000L + newCount);
            double q = totalQ(r, c, a, drunk);

            boolean better = best == -1
                    || (fresh && !bestFresh)
                    || (fresh == bestFresh && q > bestQ + 1e-6)
                    || (fresh == bestFresh && Math.abs(q - bestQ) <= 1e-6 && random.nextBoolean());
            if (better) {
                best = a;
                bestFresh = fresh;
                bestQ = q;
            }
        }
        return best == -1 ? 0 : best;
    }

    public List<Point> getBestPath() {
        resetGridToInitial();

        List<Point> path = new ArrayList<>();
        int currR = startPos.x;
        int currC = startPos.y;
        path.add(new Point(currR, currC));

        int rows = grid.length;
        int cols = grid[0].length;
        int maxSteps = rows * cols * 2;

        boolean[] drunk = new boolean[waterCount];
        int drunkCount = 0;

        Set<Long> visitedStates = new HashSet<>();
        visitedStates.add(((long) currR * cols + currC) * 100_000L + drunkCount);

        for (int step = 0; step < maxSteps; step++) {
            if (currR == cheesePos.x && currC == cheesePos.y) break;

            int bestAction = getPathAction(currR, currC, drunk, drunkCount, visitedStates);
            int nextR = currR + DR[bestAction];
            int nextC = currC + DC[bestAction];

            if (!isFree(nextR, nextC)) break;

            currR = nextR;
            currC = nextC;
            path.add(new Point(currR, currC));

            int id = waterId[currR][currC];
            if (id >= 0 && !drunk[id]) {
                drunk[id] = true;
                drunkCount++;
            }

            if (!visitedStates.add(((long) currR * cols + currC) * 100_000L + drunkCount)) break;
        }

        return path;
    }

    public CellType[][] getGrid() {
        return grid;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public Point getStartPos() {
        return startPos;
    }

    public Point getCheesePos() {
        return cheesePos;
    }

    public double[][][] getQTable() {
        int rows = grid.length;
        int cols = grid[0].length;
        double[][][] view = new double[rows][cols][4];
        boolean[] none = new boolean[waterCount];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == CellType.WALL) continue;
                for (int a = 0; a < 4; a++) {
                    if (!isFree(r + DR[a], c + DC[a])) {
                        view[r][c][a] = -config.rewardY;
                    } else if (tried[r][c][a]) {
                        view[r][c][a] = totalQ(r, c, a, none);
                    }
                }
            }
        }
        return view;
    }
}