package jp.ntrip.client;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class SettingController {
    private PropertiesUtil prop;

    @FXML private CheckBox CheckBox_UseProxy;
    @FXML private TextField TextField_ProxyAddress;
    @FXML private TextField TextField_ProxyPort;
    @FXML private Button Button_Apply;
    @FXML private Button Button_Cancel;

    public void setProperties(PropertiesUtil prop) {
        this.prop = prop;

        if(this.prop.getProperty("UseProxy").equals("On")) {
            CheckBox_UseProxy.setSelected(true);
        }
        TextField_ProxyAddress.setText(this.prop.getProperty("ProxyAddress"));
        TextField_ProxyPort.setText(this.prop.getProperty("ProxyPort"));
    }

    @FXML
    private void onApplyButtonClicked() {
        String proxyCheck;

        if(CheckBox_UseProxy.isSelected()) {
            proxyCheck = "On";
        } else {
            proxyCheck = "Off";
        }
        prop.setProperty("UseProxy", proxyCheck);
        prop.setProperty("ProxyAddress", TextField_ProxyAddress.getText());
        prop.setProperty("ProxyPort", TextField_ProxyPort.getText());

        Scene scene = Button_Apply.getScene();
        scene.getWindow().hide();
    }

    @FXML
    private void onCancelButtonClicked() {
        Scene scene = Button_Cancel.getScene();
        scene.getWindow().hide();
    }
}
