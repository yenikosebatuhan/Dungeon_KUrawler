package com.kurawler.ui;

import com.kurawler.engine.AudioManager;
import com.kurawler.model.object.Item;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Modal popup shown at the beginning of a new game.
 * Informs the player about the target item they need to find to win.
 * <p>
 * Implements PDF §4.1.2: "a random valuable item is shown to the player
 * as target item and he/she is asked to find the item."
 */
public class TargetItemPopup {

    private final Stage ownerStage;
    private final Item targetItem;

    public TargetItemPopup(Stage ownerStage, Item targetItem) {
        this.ownerStage = ownerStage;
        this.targetItem = targetItem;
    }

    /** Shows the popup as a modal dialog (blocks until dismissed). */
    public void show() {
        if (targetItem == null) return;

        Stage popup = new Stage();
        popup.initOwner(ownerStage);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        final String accent = "#e8c84a";   // golden yellow
        final String bgTint = "#2a1a1a";   // dark crimson
        final int W = 460;
        final int H = 340;

        // --- background canvas with dungeon wall tiles ---
        Canvas bg = new Canvas(W, H);
        GraphicsContext gc = bg.getGraphicsContext2D();
        gc.setFill(Color.web("#0a0a10"));
        gc.fillRect(0, 0, W, H);
        try {
            SpriteSheet wallSheet = new SpriteSheet("/assets/walls_x3.png", 48);
            if (wallSheet.isLoaded()) {
                for (int y = 0; y < H; y += 48) {
                    for (int x = 0; x < W; x += 48) {
                        int hash = Math.abs((x * 73856093) ^ (y * 19349663));
                        Image tile = wallSheet.getSprite(hash % 5, 1);
                        if (tile != null) gc.drawImage(tile, x, y, 48, 48);
                    }
                }
            }
        } catch (Exception ignore) {
            // sprite sheet not found — plain background is fine
        }
        gc.setFill(new javafx.scene.paint.RadialGradient(
                0, 0, W / 2.0, H * 0.4, Math.max(W, H) * 0.7, false,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web(bgTint, 0.55)),
                new javafx.scene.paint.Stop(1, Color.web("#000000", 0.9))));
        gc.fillRect(0, 0, W, H);
        gc.setStroke(Color.web("#8b7530", 0.5));
        gc.setLineWidth(1);
        gc.strokeRoundRect(7, 7, W - 14, H - 14, 8, 8);

        // --- title: YOUR QUEST ---
        Label titleLabel = new Label("☠  YOUR QUEST  ☠");
        titleLabel.setFont(Font.font("Serif", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.web(accent));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setPrefWidth(W - 40);
        titleLabel.setMaxWidth(W - 40);
        titleLabel.setStyle("-fx-text-alignment: center;");
        InnerShadow engrave = new InnerShadow(3, 1, 2, Color.web("#000000", 0.6));
        DropShadow glow = new DropShadow(20, Color.web(accent, 0.5));
        glow.setInput(engrave);
        titleLabel.setEffect(glow);

        // --- target item name (highlighted) ---
        Label itemLabel = new Label(targetItem.getName());
        itemLabel.setFont(Font.font("Serif", FontWeight.BOLD, 24));
        itemLabel.setTextFill(Color.web("#ffd54f"));
        itemLabel.setAlignment(Pos.CENTER);
        itemLabel.setPrefWidth(W - 40);
        itemLabel.setMaxWidth(W - 40);
        itemLabel.setStyle("-fx-text-alignment: center;");
        itemLabel.setEffect(new DropShadow(8, 0, 0, Color.web(accent, 0.7)));

        // --- main quest message ---
        Label msgLabel = new Label(
            "A legendary artifact lies hidden somewhere\n"
            + "in this dungeon. Find the\n"
            + "» " + targetItem.getName() + " «\n"
            + "to claim victory.\n\n"
            + "Beware: enemies will hunt you,\n"
            + "and your strength is finite."
        );
        msgLabel.setFont(Font.font("Serif", 14));
        msgLabel.setTextFill(Color.web("#c8b880"));
        msgLabel.setWrapText(true);
        msgLabel.setAlignment(Pos.CENTER);
        msgLabel.setTextAlignment(TextAlignment.CENTER);
        msgLabel.setPrefWidth(W - 80);
        msgLabel.setMaxWidth(W - 80);
        msgLabel.setStyle("-fx-text-alignment: center;");
        msgLabel.setEffect(new DropShadow(4, 0, 1, Color.BLACK));

        // --- begin button ---
        Button beginBtn = new Button("Begin Adventure");
        beginBtn.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        beginBtn.setPrefWidth(220);
        beginBtn.setPrefHeight(42);
        final String base = "-fx-background-color: "
                + "linear-gradient(to bottom, #4a4438, #2e2a22, #1e1b16, #2a2720); "
                + "-fx-text-fill: " + accent + "; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + "88 #3a3220 #3a3220 " + accent + "88; "
                + "-fx-border-width: 2; -fx-cursor: hand;";
        final String hover = "-fx-background-color: "
                + "linear-gradient(to bottom, #5a5448, #3e3a30, #2a2720, #353028); "
                + "-fx-text-fill: #ffffff; "
                + "-fx-background-radius: 4; -fx-border-radius: 4; "
                + "-fx-border-color: " + accent + " " + accent + "aa " + accent + "aa " + accent + "; "
                + "-fx-border-width: 2; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, " + accent + "44, 10, 0, 0, 0);";
        beginBtn.setStyle(base);
        beginBtn.setOnMouseEntered(e -> beginBtn.setStyle(hover));
        beginBtn.setOnMouseExited(e -> beginBtn.setStyle(base));
        beginBtn.setOnAction(e -> {
            try { AudioManager.getInstance().play("button"); } catch (Exception ignore) { }
            popup.close();
        });

        // --- layout ---
        VBox content = new VBox(14, titleLabel, itemLabel, msgLabel, beginBtn);
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(true);
        content.setPadding(new javafx.geometry.Insets(24, 20, 20, 20));

        StackPane root = new StackPane(bg, content);
        root.setStyle("-fx-border-color: " + accent + "; -fx-border-width: 3;");

        Scene scene = new Scene(root, W, H);
        popup.setScene(scene);
        popup.setResizable(false);

        // Allow ENTER / SPACE / ESC to dismiss the popup
        scene.setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.ENTER || k == KeyCode.SPACE || k == KeyCode.ESCAPE) {
                popup.close();
            }
        });

        // Center the popup on the owner stage (game window), not the screen.
        popup.setOnShown(e -> {
            if (ownerStage != null) {
                popup.setX(ownerStage.getX() + (ownerStage.getWidth() - popup.getWidth()) / 2);
                popup.setY(ownerStage.getY() + (ownerStage.getHeight() - popup.getHeight()) / 2);
            } else {
                popup.centerOnScreen();
            }
        });

        popup.showAndWait();
    }
}
