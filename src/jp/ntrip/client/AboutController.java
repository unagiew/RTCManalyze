package jp.ntrip.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;

public class AboutController {
    @FXML private Button Button_Close;

    @FXML
    private void onCloseButtonClicked(ActionEvent event) {
        Scene scene = Button_Close.getScene();
        scene.getWindow().hide();
    }
}
