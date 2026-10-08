package io.github.ikyral.lab2.maze.gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class ConfigDialog extends Dialog<AppConfig> {

    public ConfigDialog(AppConfig initial) {
        setTitle("Параметры задачи и Q-Learning");
        setHeaderText("Задайте параметры лабиринта и обучения (пустые поля воды/молний считаются автоматически)");

        ButtonType applyButtonType = new ButtonType("Применить и создать", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 40, 10, 10));

        TextField tfWidth = new TextField(String.valueOf(initial.widthRooms));
        TextField tfHeight = new TextField(String.valueOf(initial.heightRooms));
        TextField tfZ = new TextField(String.valueOf(initial.rewardZ));
        TextField tfX = new TextField(String.valueOf(initial.rewardX));
        TextField tfY = new TextField(String.valueOf(initial.rewardY));

        TextField tfWaterCount = new TextField(initial.customWaterCount == null ? "" : String.valueOf(initial.customWaterCount));
        tfWaterCount.setPromptText("Авто (по размеру карты)");

        TextField tfShockCount = new TextField(initial.customShockCount == null ? "" : String.valueOf(initial.customShockCount));
        tfShockCount.setPromptText("Авто (по размеру карты)");

        TextField tfAlpha = new TextField(String.valueOf(initial.alpha));
        TextField tfGamma = new TextField(String.valueOf(initial.gamma));

        grid.addRow(0, new Label("Комнат по горизонтали (X):"), tfWidth);
        grid.addRow(1, new Label("Комнат по вертикали (Y):"), tfHeight);
        grid.addRow(2, new Label("Награда за Сыр (+Z):"), tfZ);
        grid.addRow(3, new Label("Награда за Воду (+x):"), tfX);
        grid.addRow(4, new Label("Штраф за Ток/Стену (-y):"), tfY);
        grid.addRow(5, new Label("Количество Воды (💧):"), tfWaterCount);
        grid.addRow(6, new Label("Количество Молний (⚡):"), tfShockCount);
        grid.addRow(7, new Label("Скорость обучения (α):"), tfAlpha);
        grid.addRow(8, new Label("Дисконтирование (γ):"), tfGamma);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                AppConfig cfg = new AppConfig();
                cfg.widthRooms = Integer.parseInt(tfWidth.getText().trim());
                cfg.heightRooms = Integer.parseInt(tfHeight.getText().trim());
                cfg.rewardZ = Double.parseDouble(tfZ.getText().trim());
                cfg.rewardX = Double.parseDouble(tfX.getText().trim());
                cfg.rewardY = Double.parseDouble(tfY.getText().trim());

                String waterInput = tfWaterCount.getText().trim();
                cfg.customWaterCount = waterInput.isEmpty() ? null : Math.max(0, Integer.parseInt(waterInput));

                String shockInput = tfShockCount.getText().trim();
                cfg.customShockCount = shockInput.isEmpty() ? null : Math.max(0, Integer.parseInt(shockInput));

                cfg.alpha = Double.parseDouble(tfAlpha.getText().trim());
                cfg.gamma = Double.parseDouble(tfGamma.getText().trim());
                return cfg;
            }
            return null;
        });
    }
}
