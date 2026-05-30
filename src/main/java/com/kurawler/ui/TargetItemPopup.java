package com.kurawler.ui;

import com.kurawler.model.object.Item;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class TargetItemPopup {

    private final Stage owner;
    private final Item targetItem;

    public TargetItemPopup(Stage owner, Item targetItem) {
        this.owner = owner;
        this.targetItem = targetItem;
    }

    /** Blocks until the player dismisses the popup (uses showAndWait). */
    public void show() {
        if (targetItem == null) return;

        Stage popup = new Stage();
        popup.initOwner(owner);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        Label title = new Label("YOUR QUEST");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#e8c84a"));
        title.setEffect(new javafx.scene.effect.DropShadow(6, 0, 2, Color.BLACK));

        Label msg = new Label("Find the " + targetItem.getName() + "!");
        msg.setFont(Font.font("Serif", 16));
        msg.setTextFill(Color.web("#c8b880"));

        Button okBtn = new Button("Begin Adventure");
        okBtn.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        okBtn.setPrefWidth(200);
        okBtn.setPrefHeight(40);
        okBtn.setStyle("-fx-background-color: linear-gradient(to bottom,#2a2418,#15110c); "
                + "-fx-text-fill: #e8c84a; -fx-border-color: #8b7940; "
                + "-fx-border-width: 2; -fx-cursor: hand; -fx-background-radius:4; -fx-border-radius:4;");
        okBtn.setOnAction(e -> popup.close());

        VBox root = new VBox(20, title, msg, okBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #12100c; -fx-border-color: #c8a840; "
                + "-fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6;");

        popup.setScene(new Scene(root, 340, 200));
        popup.setOnShown(e -> {
            popup.setX(owner.getX() + (owner.getWidth()  - 340) / 2);
            popup.setY(owner.getY() + (owner.getHeight() - 200) / 2);
        });
        popup.showAndWait();
    }
}
