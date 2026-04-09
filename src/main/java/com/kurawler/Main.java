package com.kurawler;

import com.kurawler.ui.ScreenManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point — launches the Main Menu via ScreenManager.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        ScreenManager screenManager = new ScreenManager(stage);
        screenManager.showMainMenu();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
