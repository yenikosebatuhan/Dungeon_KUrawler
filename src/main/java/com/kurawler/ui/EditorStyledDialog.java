package com.kurawler.ui;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dark/gold modal dialogs matching the level editor and in-game popups.
 */
public final class EditorStyledDialog {

    private static final String BG = "#0e0c0a";
    private static final String GOLD = "#e8c84a";
    private static final String GOLD_DIM = "#c8b880";
    private static final String BORDER = "#c8a840";
    private static final String FIELD_BG = "#1a1a2e";

    private EditorStyledDialog() {
    }

    public static Optional<String> promptText(Stage owner, String title, String subtitle,
                                              String defaultValue) {
        AtomicReference<String> result = new AtomicReference<>(null);

        Stage popup = basePopup(owner);
        int w = 420;
        int h = 220;

        StackPane root = themedRoot(w, h);

        Label titleLbl = titleLabel(title);
        Label subLbl = subtitleLabel(subtitle);

        TextField field = new TextField(defaultValue != null ? defaultValue : "");
        field.setFont(Font.font("Monospaced", 14));
        field.setPrefWidth(340);
        field.setStyle("-fx-background-color: " + FIELD_BG + "; "
                + "-fx-text-fill: #e0e0e0; -fx-border-color: " + BORDER + "; "
                + "-fx-border-width: 1.5; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-padding: 8;");

        Button saveBtn = dialogButton("Save", "#55efc4");
        Button cancelBtn = dialogButton("Cancel", "#a09070");

        Runnable confirm = () -> {
            String text = field.getText();
            if (text != null && !text.trim().isEmpty()) {
                result.set(text.trim());
            }
            popup.close();
        };

        saveBtn.setOnAction(e -> confirm.run());
        cancelBtn.setOnAction(e -> popup.close());
        field.setOnAction(e -> confirm.run());

        HBox buttons = new HBox(12, saveBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(14, titleLbl, subLbl, field, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        root.getChildren().add(content);

        Scene scene = new Scene(root, w, h);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                popup.close();
            } else if (e.getCode() == KeyCode.ENTER) {
                confirm.run();
            }
        });
        finishPopup(popup, owner, scene);
        field.requestFocus();
        field.selectAll();

        return Optional.ofNullable(result.get());
    }

    public static Optional<Integer> chooseIndex(Stage owner, String title, String subtitle,
                                                List<String> options) {
        if (options == null || options.isEmpty()) {
            return Optional.empty();
        }

        AtomicReference<Integer> result = new AtomicReference<>(null);

        Stage popup = basePopup(owner);
        int w = 440;
        int h = Math.min(420, 180 + options.size() * 32);

        StackPane root = themedRoot(w, h);

        Label titleLbl = titleLabel(title);
        Label subLbl = subtitleLabel(subtitle);

        ListView<String> list = new ListView<>();
        list.getItems().addAll(options);
        list.getSelectionModel().select(0);
        list.setPrefHeight(h - 140);
        list.setPrefWidth(360);
        list.setStyle("-fx-background-color: " + FIELD_BG + "; "
                + "-fx-control-inner-background: " + FIELD_BG + "; "
                + "-fx-text-fill: #e0e0e0; -fx-border-color: " + BORDER + "; "
                + "-fx-border-width: 1;");

        Button loadBtn = dialogButton("Load", "#55efc4");
        Button cancelBtn = dialogButton("Cancel", "#a09070");

        Runnable confirm = () -> {
            int idx = list.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                result.set(idx);
            }
            popup.close();
        };

        loadBtn.setOnAction(e -> confirm.run());
        cancelBtn.setOnAction(e -> popup.close());
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                confirm.run();
            }
        });

        HBox buttons = new HBox(12, loadBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(12, titleLbl, subLbl, list, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        root.getChildren().add(content);

        Scene scene = new Scene(root, w, h);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                popup.close();
            } else if (e.getCode() == KeyCode.ENTER) {
                confirm.run();
            }
        });
        finishPopup(popup, owner, scene);

        return Optional.ofNullable(result.get());
    }

    public static void showMessage(Stage owner, String title, String message) {
        Stage popup = basePopup(owner);
        int w = 380;
        int h = 180;

        StackPane root = themedRoot(w, h);

        Label titleLbl = titleLabel(title);
        Label msgLbl = subtitleLabel(message);
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(320);
        msgLbl.setAlignment(Pos.CENTER);

        Button okBtn = dialogButton("OK", GOLD);
        okBtn.setOnAction(e -> popup.close());

        VBox content = new VBox(16, titleLbl, msgLbl, okBtn);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(22));
        root.getChildren().add(content);

        Scene scene = new Scene(root, w, h);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE) {
                popup.close();
            }
        });
        finishPopup(popup, owner, scene);
    }

    private static Stage basePopup(Stage owner) {
        Stage popup = new Stage();
        if (owner != null) {
            popup.initOwner(owner);
        }
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        return popup;
    }

    private static StackPane themedRoot(int w, int h) {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        Canvas bg = new Canvas(w, h);
        GraphicsContext gc = bg.getGraphicsContext2D();
        gc.setFill(Color.web(BG, 0.97));
        gc.fillRoundRect(0, 0, w, h, 10, 10);
        gc.setStroke(Color.web(BORDER));
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(2, 2, w - 4, h - 4, 10, 10);
        gc.setStroke(Color.web("#8b7530", 0.45));
        gc.setLineWidth(1);
        gc.strokeRoundRect(8, 8, w - 16, h - 16, 8, 8);

        root.getChildren().add(bg);
        return root;
    }

    private static Label titleLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));
        label.setTextFill(Color.web(GOLD));
        label.setEffect(new DropShadow(4, 0, 1, Color.BLACK));
        return label;
    }

    private static Label subtitleLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Monospaced", 12));
        label.setTextFill(Color.web(GOLD_DIM));
        return label;
    }

    private static Button dialogButton(String text, String accent) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        btn.setPrefWidth(120);
        btn.setPrefHeight(34);
        String base = "-fx-background-color: linear-gradient(to bottom, #2a1a3e, #1a1028); "
                + "-fx-text-fill: " + accent + "; -fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + "88; -fx-border-width: 1.5; -fx-cursor: hand;";
        String hover = "-fx-background-color: linear-gradient(to bottom, #3a2a4e, #2a1a38); "
                + "-fx-text-fill: #ffffff; -fx-border-color: " + accent + "; "
                + "-fx-effect: dropshadow(gaussian, " + accent + "55, 8, 0, 0, 0);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private static void finishPopup(Stage popup, Stage owner, Scene scene) {
        popup.setScene(scene);
        popup.setResizable(false);
        popup.setOnShown(e -> {
            if (owner != null && owner.isShowing()) {
                popup.setX(owner.getX() + (owner.getWidth() - popup.getWidth()) / 2);
                popup.setY(owner.getY() + (owner.getHeight() - popup.getHeight()) / 2);
            } else {
                popup.centerOnScreen();
            }
        });
        popup.showAndWait();
    }
}
