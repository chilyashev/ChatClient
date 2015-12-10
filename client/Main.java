package client;

import client.ui.ScreenContainer;
import client.ui.screens.LoginController;
import client.ui.screens.MainController;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Scanner;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        ScreenContainer mainScreen = new ScreenContainer();

        // Adding the screens
        mainScreen.loadScreen("login", "/screens/client/login.fxml");
        mainScreen.loadScreen("main", "/screens/client/main.fxml");


        // Showing the main screen
        mainScreen.showScreen("login");
        primaryStage.setOnCloseRequest(event -> {
            mainScreen.closeScreen();
        });
        // Displaying the stage
        Group root = new Group();
        root.getChildren().addAll(mainScreen);
        Scene scene = new Scene(root);//, root.getLayoutBounds().getWidth(), root.getLayoutBounds().getHeight());
        primaryStage.setScene(scene);
        primaryStage.minHeightProperty().bind(mainScreen.heightProperty());
        primaryStage.minWidthProperty().bind(mainScreen.widthProperty());
        primaryStage.setTitle("Chat Client");
        primaryStage.show();

    }


    public static void main(String[] args) {
        launch(args);
    }

    public static void smain(String[] args) {
        LoginController lc = new LoginController();

        System.err.print("waiting... ");
        Scanner s = new Scanner(System.in);
        //   s.nextLine();
    }
}