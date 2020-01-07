package jp.ntrip.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {
    private static final String PROPERTIES_INITFILEPATH = "./ntrip.properties";
    private static final int PROPERTIESLIMIT = 5;

    private PropertiesUtil prop = new PropertiesUtil(PROPERTIES_INITFILEPATH);
    SocketThread socketThread;
    private Stage stage;
    FxField fxField;

    @FXML private FlowPane FlowPane_Chart;
    @FXML private VBox VBox_Info;
    @FXML private TableView<InfoTable> TableView_Info;
    @FXML private TableColumn<InfoTable, String> TableColumn_Item;
    @FXML private TableColumn<InfoTable, String> TableColumn_Value;
    @FXML private TableColumn<InfoTable, String> TableColumn_Times;
    @FXML private TextField TextField_Latitude;
    @FXML private TextField TextField_Longitude;
    @FXML private TextField TextField_User;
    @FXML private PasswordField PasswordField_Password;
    @FXML private TextFlow TextFlow_Stream;
    @FXML private TextFlow TextFlow_Analyzed;
    @FXML private ComboBox<String> ComboBox_Host;
    @FXML private ComboBox<String> ComboBox_MountPoint;
    @FXML private CheckBox CheckBox_NMEA;
    @FXML private CheckBox CheckBox_UserPass;
    @FXML private Button Button_Start;
    @FXML private Button Button_Stop;
    @FXML private ScrollPane ScrollPane_Vbox;
    @FXML private ScrollPane ScrollPane_Stream;
    @FXML private ScrollPane ScrollPane_Analyzed;
    @FXML private Label Label_Error;

    /**
     * 親windowのstage setter<br>
     * 子windowのwidthを合わせるためにstageが必要。
     * @param stage ステージ
     */
    public void setStage(Stage stage) {
        this.stage = stage;

        // TextFlowの幅を親ステージの幅にバインドする
        // initialize時は親ステージが不明のため、setter呼び出し時に設定する。
        TextFlow_Analyzed.prefWidthProperty().bind(stage.widthProperty());
        VBox_Info.prefWidthProperty().bind(stage.widthProperty());
    }

    @FXML
    private void initialize() {
        // scrollPaneのスクロールをTextFlowと連携させる
        // TextFlowにtextが追加されたらスクロールを一番下まで下げる。
        TextFlow_Stream.getChildren().addListener((ListChangeListener<Node>) ((change) -> {
            ScrollPane_Stream.layout();
            ScrollPane_Stream.setVvalue(1.0f);
        }));
        TextFlow_Analyzed.getChildren().addListener((ListChangeListener<Node>) ((change) -> {
            ScrollPane_Analyzed.layout();
            ScrollPane_Analyzed.setVvalue(1.0f);
        }));

        //  TableViewの列とinfoTableクラスを連携させる
        TableColumn_Item.setCellValueFactory(new PropertyValueFactory<>("item"));
        TableColumn_Value.setCellValueFactory(new PropertyValueFactory<>("value"));
        TableColumn_Times.setCellValueFactory(new PropertyValueFactory<>("times"));

        fxField = setFxField();

        // propertiesファイル読み込み、フィールド埋め
        prop.readPropertiesFile();
        setFieldFromProperties();
    }

    @FXML
    private void onStartButtonClicked() {
        // フィールドクリア
        TextFlow_Stream.getChildren().clear();
        TextFlow_Analyzed.getChildren().clear();
        TableView_Info.getItems().clear();
        FlowPane_Chart.getChildren().clear();

        // HTTP Request生成
        HttpRequest request = new HttpRequest(fxField);
        if(!request.setHostAndPath(ComboBox_Host.getValue(), ComboBox_MountPoint.getValue())) {
            return;
        }
        if(prop.getProperty("UseProxy").equals("On")) {
            if(!request.setProxy(prop.getProperty("ProxyAddress"), prop.getProperty("ProxyPort"))) {
                return;
            }
        }
        request.setMethod("GET");

        // ユーザ認証設定
        if(CheckBox_UserPass.isSelected()) {
            request.setBasicAuth(TextField_User.getText(), PasswordField_Password.getText());
        }

        socketThread = new SocketThread(fxField);
        socketThread.setRequest(request);

        // スレッド開始時にスタートボタンを使用不可とする
        Label_Error.setText("");
        Button_Start.setText("Connecting");
        Button_Start.setStyle("-fx-base: goldenrod;");
        Button_Start.setDisable(true);

        socketThread.restart();
    }

    @FXML
    private void onStopButtonClicked() {
        if(socketThread != null) {
            socketThread.disconnect();
        }

        // コンボボックスに設定値を追加する
        List<ComboBox<String>> targets = Arrays.asList(ComboBox_Host, ComboBox_MountPoint);
        for(ComboBox<String> target : targets) {
            String currentValue = target.getValue();
            ObservableList<String> values = target.getItems();
            values.remove(currentValue);
            values.add(0, currentValue);

            if(values.size() > PROPERTIESLIMIT) {
                values.remove(PROPERTIESLIMIT, values.size());
            }
            target.setValue(currentValue);
        }
    }

    @FXML
    private void onActionMenuClose() {
        Platform.exit();
    }

    @FXML
    private void onActionMenuAbout() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("aboutWindow.fxml"));
        Scene scene = null;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(this.stage);
        stage.setTitle("About");
        stage.show();
    }

    @FXML
    private void onActionMenuSetting() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("settingWindow.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SettingController controller = loader.getController();
        controller.setProperties(prop);

        assert root != null;
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(this.stage);
        stage.setTitle("Setting");
        stage.show();
    }

    private FxField setFxField() {
        FxField fxField = new FxField();

        fxField.setHexText(TextFlow_Stream);
        fxField.setAnalyzedText(TextFlow_Analyzed);
        fxField.setErrorLabel(Label_Error);
        fxField.setInformationTable(TableView_Info);
        fxField.setStartButton(Button_Start);
        fxField.setPaneForChart(FlowPane_Chart);

        return fxField;
    }

    private void setFieldFromProperties() {
        List<String> hosts = prop.getProperties("Hosts");
        List<String> points = prop.getProperties("MountPoints");

        for(String host : hosts) {
            ComboBox_Host.getItems().add(host);
        }

        for(String point : points) {
            ComboBox_MountPoint.getItems().add(point);
        }

        if(this.prop.getProperty("UseNMEA").equals("On")) {
            CheckBox_NMEA.setSelected(true);
        }
        TextField_Latitude.setText(this.prop.getProperty("Latitude"));
        TextField_Longitude.setText(this.prop.getProperty("Longitude"));
    }

    private void setPropertiesFromField() {
        prop.setProperties("Hosts", ComboBox_Host.getItems());
        prop.setProperties("MountPoints", ComboBox_MountPoint.getItems());

        String nmeaCheck;
        if(CheckBox_NMEA.isSelected()) {
            nmeaCheck = "On";
        } else {
            nmeaCheck = "Off";
        }
        prop.setProperty("UseNMEA", nmeaCheck);
        prop.setProperty("Latitude", TextField_Latitude.getText());
        prop.setProperty("Longitude", TextField_Longitude.getText());

        prop.writeProperties();
    }

    /**
     * 各Fieldの内容をproperties（file）に書き込む。<br>
     * windowクローズされたときの親クラスからの実行のため
     */
    public void actionBeforeCloseController() {
        setPropertiesFromField();
    }
}


class FxField {
    private TextFlow hexText;
    private TextFlow analyzedText;
    private Label errorLabel;
    private Button startButton;
    private TableView<InfoTable> informationTable;
    private FlowPane paneForChart;

    TextFlow getHexText() {
        return hexText;
    }
    void setHexText(TextFlow hexText) {
        this.hexText = hexText;
    }
    TextFlow getAnalyzedText() {
        return analyzedText;
    }
    void setAnalyzedText(TextFlow analyzedText) {
        this.analyzedText = analyzedText;
    }
    Label getErrorLabel() {
        return errorLabel;
    }
    void setErrorLabel(Label errorLabel) {
        this.errorLabel = errorLabel;
    }
    Button getStartButton() {
        return startButton;
    }
    void setStartButton(Button startButton) {
        this.startButton = startButton;
    }
    TableView<InfoTable> getInformationTable() {
        return informationTable;
    }
    void setInformationTable(TableView<InfoTable> informationTable) {
        this.informationTable = informationTable;
    }
    FlowPane getPaneForChart() {
        return paneForChart;
    }
    void setPaneForChart(FlowPane paneForChart) {
        this.paneForChart = paneForChart;
    }
}
