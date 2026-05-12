package com.kurawler.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class HelpScreen {
    private final ScreenManager screenManager;

    public HelpScreen(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    public Scene createScene() {
        Label title = new Label("HELP & CONTROLS");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#e8d44d"));
        title.setEffect(new DropShadow(12, Color.web("#e8d44d", 0.4)));
        title.setTextAlignment(TextAlignment.CENTER);

        VBox contentBox = new VBox(6);
        contentBox.setPadding(new Insets(12));
        addSection(contentBox, "\u2694 GOAL",
                "Find the target artifact shown in the sidebar.\n"
                + "It is hidden inside a searchable object (marked with a glowing dot).");
        addSection(contentBox, "\u2B06 MOVEMENT",
                "W/A/S/D or Arrow Keys: Move one tile\n"
                + "ESC: Open in-game menu");
        addSection(contentBox, "\u270B INTERACTION",
                "Click an adjacent tile to:\n"
                + "  \u2022 TAKE items from the ground\n"
                + "  \u2022 SEARCH searchable objects\n"
                + "  \u2022 BREAK destructible objects\n"
                + "  \u2022 ATTACK enemies");
        addSection(contentBox, "\u26A1 COMBAT",
                "F: Shoot a ranged bolt (costs 10 MP)\n"
                + "Click an adjacent enemy to melee attack\n"
                + "E: Use a health potion");
        addSection(contentBox, "\u25BC INVENTORY",
                "Click an inventory slot to use context actions:\n"
                + "  Weapon  \u2192 EQUIP / DISCARD\n"
                + "  Wearable \u2192 WEAR / REMOVE\n"
                + "  Consumable \u2192 EAT\n"
                + "  Scroll \u2192 READ (heals + removes)");
        addSection(contentBox, "\u2620 ENEMIES",
                "Knights: Chase and attack in melee range.\n"
                + "Sorcerers: Teleport and cast projectiles with line of sight.");
        addSection(contentBox, "\u2764 WIN / LOSE",
                "HP = 0 \u2192 Game Over\n"
                + "Find the artifact \u2192 Victory!\n"
                + "Pause button freezes time and enemies.");

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        scrollPane.setStyle("-fx-background: #111124; -fx-background-color: #111124; "
                + "-fx-border-color: #333355;");

        Button backBtn = new Button("\u2190 Back to Menu");
        backBtn.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        backBtn.setPrefWidth(200);
        String base = "-fx-background-color: #3a7c5f; -fx-text-fill: #f0f0f0; "
                + "-fx-background-radius: 5; -fx-border-radius: 5; -fx-cursor: hand;";
        String hover = "-fx-background-color: #4aa870; -fx-text-fill: #ffffff; "
                + "-fx-background-radius: 5; -fx-border-radius: 5; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, #4aa87088, 8, 0, 0, 0);";
        backBtn.setStyle(base);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(hover));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(base));
        backBtn.setOnAction(e -> screenManager.showMainMenu());

        VBox layout = new VBox(16, title, scrollPane, backBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0a1a, #141428, #0f0f23);");

        return new Scene(layout, 580, 560);
    }

    private void addSection(VBox parent, String heading, String body) {
        Label h = new Label(heading);
        h.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        h.setTextFill(Color.web("#6fc3df"));

        Label b = new Label(body);
        b.setFont(Font.font("Monospaced", 12));
        b.setTextFill(Color.web("#ccccdd"));
        b.setWrapText(true);
        b.setLineSpacing(2);
        b.setPadding(new Insets(0, 0, 8, 8));

        parent.getChildren().addAll(h, b);
    }
}
