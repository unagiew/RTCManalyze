package jp.ntrip.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NtripClient extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainWindow.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setStage(primaryStage);	// 親ステージをcontrollerに渡す

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("NtripClient.css").toExternalForm());

        // windowクローズしたときの動作
        primaryStage.showingProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue && !newValue) {
                controller.actionBeforeCloseController();
            }
        });

        primaryStage.setTitle("NTRIP Client");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.show();
    }
}