module io.github.ikyral.lab2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens io.github.ikyral.lab2.maze.gui to javafx.fxml;

    exports io.github.ikyral.lab2.app;
    exports io.github.ikyral.lab2.maze.gui;
}